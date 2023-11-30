package me.coley.recaf.parse;

import com.github.javaparser.*;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import java.util.Arrays;
import me.coley.recaf.Controller;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.code.LiteralExpressionInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.parse.evaluation.ExpressionEvaluator;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Utility for working with JavaParser and pulling contextual information from Java source code.
 *
 * @author Matt Coley
 */
public class JavaParserHelper {

	private static final Logger logger = Logging.get(JavaParserHelper.class);

	private final WorkspaceSymbolSolver symbolSolver;
	private final JavaParser parser;

	private JavaParserHelper(WorkspaceSymbolSolver symbolSolver) {
		this.symbolSolver = symbolSolver;
		parser = new JavaParser(new ParserConfiguration()
				.setLanguageLevel(LanguageLevel.JAVA_16)
				.setSymbolResolver(this.symbolSolver));
	}

	/**
	 * @param controller
	 * 		Recaf controller, used to pull a {@link WorkspaceTypeSolver}.
	 *
	 * @return Instance of JavaParser helper utility.
	 */
	public static JavaParserHelper create(Controller controller) {
		return create(controller.getServices().getSymbolSolver());
	}

	/**
	 * @param symbolSolver
	 * 		Type solver for JavaParser integration with types recognized by Recaf.
	 *
	 * @return Instance of JavaParser helper utility.
	 */
	public static JavaParserHelper create(WorkspaceSymbolSolver symbolSolver) {
		return new JavaParserHelper(symbolSolver);
	}

	/**
	 * @param code
	 * 		Code to parse.
	 *
	 * @return {@link ParseResult} wrapper around a {@link CompilationUnit} result.
	 */
	public ParseResult<CompilationUnit> parseClass(String code) {
		return parseClass(code, true);
	}

	/**
	 * @param code
	 * 		Code to parse.
	 * @param tryRecover
	 * 		Set to {@code true} to attempt to recover parse failures.
	 *
	 * @return {@link ParseResult} wrapper around a {@link CompilationUnit} result.
	 */
	public ParseResult<CompilationUnit> parseClass(String code, boolean tryRecover) {
		code = filterGenerics(code);
		ParseResult<CompilationUnit> result = parser.parse(code);
		List<Problem> problems = result.getProblems();
		if (result.getResult().isPresent()) {
			// There is a compilation unit, but is it usable?
			CompilationUnit unit = result.getResult().get();
			if (tryRecover && isInvalidCompilationUnit(unit)) {
				logger.info("{} problems found when parsing class", result.getProblems().size());
					for (Problem problem : result.getProblems()) {
					logger.info("\t - {}", problem);
				}
				// Its not usable at all, attempt to recover
				return new JavaParserRecovery(this).parseClassWithRecovery(code, problems);
			} else {
				// The unit is usable, but may contain some localized problems.
				// Check if we want to try to resolve any reported problems.
				if (tryRecover && !result.getProblems().isEmpty()) {
					logger.info("{} problems found when parsing class", result.getProblems().size());
					for (Problem problem : result.getProblems()) {
						logger.info("\t - {}", problem);
					}
					return new JavaParserRecovery(this).parseClassWithRecovery(code, problems);
				}
				// Update unit and roll with whatever we got.
				updateUnitMetaData(unit);
			}
		} else if (tryRecover) {
			// No unit in result, attempt to recover
			return new JavaParserRecovery(this).parseClassWithRecovery(code, problems);
		}
		return result;
	}

	/**
	 * JavaParser {@link com.github.javaparser.resolution.types.ResolvedReferenceType} will fail if there
	 * is a mismatch in generic type arguments. If we're resolving from workspace references, those do not get
	 * their type arguments generated, so it expects {@code 0} but our decompiled source may specify {@code 1} or
	 * more. This code will match any generic type declaration (between two {@code <>} pairs).
	 * <br>
	 * Examples of valid matches:
	 * <pre>
	 *     <T>
	 *     <List<Set<String>>>
	 *     <T extends EntityLivingBase<X> | Foo>
	 * </pre>
	 * But will not match if the content is inside a string.
	 * We then replace the matched content with an equal number of spaces so that none of the text positions
	 * become offset.
	 *
	 * @param code
	 * 		Removed generics from the code by removing contents between &lt; and &gt;.
	 *
	 * @return Code with generics content replaced with spaces.
	 */
	private String filterGenerics(String code) {
        char[] codeAsCharArray = code.toCharArray();
		int nestedGenericLevel = 0;
		boolean isEscaped = false;
		boolean isString = false;
		boolean isCharEscaped = false;

		char lastChar = Character.MIN_VALUE;

		int beginReplacement = -1;
		for (int i = 0; i < codeAsCharArray.length; i++) {
			if (i > 0) lastChar = codeAsCharArray[i - 1];
			var currentChar = codeAsCharArray[i];
			if (isEscaped) {
				isEscaped = false;
				continue;
			}
			if (currentChar == '\\') {
				isEscaped = true;
			} else if (currentChar == '\"') {
				isString = !isString;
			} else {
				if (isCharEscaped) {
					if (currentChar == '\'') {
						isCharEscaped = false;
					}
					continue;
				}
				if (isString) {
					continue;
				}
				if (Character.isSpaceChar(currentChar)) continue;

				if (currentChar == '\'') {
					isCharEscaped = true;
				} else if (currentChar == '<') {
					//in generic declaration, two '<' char cannot be consecutive
					if (lastChar == '<') {
						beginReplacement = -1;
						nestedGenericLevel = 0;
					} else {
						if (beginReplacement == -1) {
							beginReplacement = i;
						}
						nestedGenericLevel++;
					}
				} else if (currentChar == '>') {
					if (beginReplacement > -1) {
						nestedGenericLevel--;
						if (nestedGenericLevel == 0) {
							Arrays.fill(codeAsCharArray, beginReplacement, i + 1, ' ');
							beginReplacement = -1;
						}
					}
				} else if (isNotAGenericDeclaration(currentChar, lastChar)) {
					//we are not a generic type declaration
					if (nestedGenericLevel > 0) {
						beginReplacement = -1;
						nestedGenericLevel = 0;
					}
				}
			}
		}
		if (nestedGenericLevel != 0) {
			System.out.println(" -- nested generic level is not 0 at EOF: " + nestedGenericLevel);
		}
		return new String(codeAsCharArray);
	}

	/**
	 * @param currentChar 
	 * 		The current char being read
	 * @param lastChar 
	 * 		The last char read
	 * @return {@code true} if we can be sure that we are not in a generic declaration; {@code false} otherwise
	 */
	private boolean isNotAGenericDeclaration(char currentChar, char lastChar) {
		if (currentChar == '[' || currentChar == ']' || currentChar == ',') return false;
        if (currentChar == '&') return lastChar == '&';
		if (currentChar == '|') return lastChar == '|';
		return ! Character.isJavaIdentifierPart((int)currentChar);
	}

	/**
	 * Adds some missing meta-data to compilation units.
	 *
	 * @param unit
	 * 		Unit to update metadata of.
	 */
	private void updateUnitMetaData(CompilationUnit unit) {
		// For some reason calling "resolve()" on some AST nodes fails becuase the symbol resolver isn't set.
		// Well, it clearly is in the config, which the parser should pass to the created unit, but doesn't.
		unit.setData(Node.SYMBOL_RESOLVER_KEY, symbolSolver);
	}

	/**
	 * @param unit
	 * 		Unit to check.
	 *
	 * @return {@code true} if the unit is not valid <i>(Parse failure that could not be recovered from)</i>.
	 */
	private boolean isInvalidCompilationUnit(CompilationUnit unit) {
		return unit.getChildNodes().isEmpty();
	}

	/**
	 * @param unit
	 * 		A parsed source tree.
	 * @param line
	 * 		Line number of source.
	 * @param column
	 * 		Column position in line.
	 *
	 * @return Matched item at the given position. Wrapped value may be:
	 * <ul>
	 *     <li>{@link me.coley.recaf.code.ClassInfo}</li>
	 *     <li>{@link me.coley.recaf.code.FieldInfo}</li>
	 *     <li>{@link me.coley.recaf.code.MethodInfo}</li>
	 * </ul>
	 */

	public Optional<ParseHitResult> at(CompilationUnit unit, int line, int column) {
		WorkspaceTypeSolver typeSolver = symbolSolver.getTypeSolver();
		if (unit == null) return Optional.empty();
		Node node = getNodeAtLocation(line, column, unit);
		if (node instanceof Expression) {
			Expression expression = (Expression) node;
			// Double does not have any conversions to apply, so we ignore it.
			// We're targeting integers primarily so that we can convert bases.
			if (!expression.isDoubleLiteralExpr()) {
				Optional<Number> nbOpt = ExpressionEvaluator.evaluate(expression);
				if (nbOpt.isPresent()) {
					return Optional.of(new ParseHitResult(new LiteralExpressionInfo(nbOpt.get(), expression), node));
				}
			}
		}
		while (node != null && JavaParserResolving.isNodeResolvable(node)) {
			ItemInfo value = JavaParserResolving.of(symbolSolver, node);
			if (value == null) {
				Optional<Node> parent = node.getParentNode();
				// Check if the parent item can be resolved
				if (parent.isEmpty()) break; // No parent, end the loop
				node = parent.get();
				continue;
			} else if (node instanceof SimpleName && value instanceof FieldInfo) {
				// In some obfuscated cases, a method name may be selected as an AST 'SimpleName'.
				// These typically resolve to fields of matching names.
				Optional<Node> parent = node.getParentNode();
				if (parent.isPresent()) {
					ItemInfo info = JavaParserResolving.of(symbolSolver, parent.get());
					// If the parent of the node is a method of the same name, we probably meant to
					// yield the method info, not a field by the same name.
					if (info instanceof MethodInfo && info.getName().equals(value.getName())) {
						value = info;
					}
				}
			}
			return Optional.of(new ParseHitResult(value, node));
		}
		// Handle edge cases like package import names.
		ItemInfo value = JavaParserResolving.ofEdgeCases(typeSolver, node);
		if (value == null)
			return Optional.empty();
		return Optional.of(new ParseHitResult(value, node));
	}

	/**
	 * @param unit
	 * 		A parsed source tree.
	 * @param line
	 * 		Line number of source.
	 * @param column
	 * 		Column position in line.
	 *
	 * @return Matched declaration item at the given position. Wrapped value may be:
	 * <ul>
	 *     <li>{@link me.coley.recaf.code.ClassInfo}</li>
	 *     <li>{@link me.coley.recaf.code.FieldInfo}</li>
	 *     <li>{@link me.coley.recaf.code.MethodInfo}</li>
	 * </ul>
	 */
	public Optional<ParseHitResult> declarationAt(CompilationUnit unit, int line, int column) {
		if (unit != null) {
			Node node = getNodeAtLocation(line, column, unit);
			while (node != null) {
				// Ensure node is a declaration of some kind (class/field/method)
				boolean isDec = (node instanceof FieldDeclaration ||
						node instanceof EnumConstantDeclaration ||
						node instanceof MethodDeclaration ||
						node instanceof ConstructorDeclaration ||
						node instanceof ClassOrInterfaceDeclaration);
				Optional<Node> parent = node.getParentNode();
				if (isDec) {
					// If we've found a declaration, make sure it's not part of an anonymous class.
					// Things defined as expressions we cannot know the class name of.
					if (parent.isPresent() && parent.get() instanceof ObjectCreationExpr) {
						node = parent.get();
					} else {
						break;
					}
				} else {
					// Try again with parent node.
					// If there is no parent, our search is over.
					if (parent.isPresent()) {
						node = parent.get();
					} else {
						break;
					}
				}
			}
			// Handle edge cases like package import names.
			if (node != null) {
				ItemInfo value = JavaParserResolving.of(symbolSolver, node);
				if (value != null) {
					return Optional.of(new ParseHitResult(value, node));
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * @param line
	 * 		Line number of source.
	 * @param column
	 * 		Column position in line.
	 * @param root
	 * 		Root node of a parsed source.
	 *
	 * @return Child of the root node that is the tightest fit to the given position. May be {@code null}.
	 */
	private static Node getNodeAtLocation(int line, int column, Node root) {
		// Ensure the node has a range
		if (!root.getBegin().isPresent())
			return null;
		// Check cursor is in bounds
		// We won't instantly return null because the root range may be SMALLER than
		// the range of children. This is really stupid IMO but thats how JavaParser is in some cases.
		boolean bounds = true;
		Position cursor = new Position(line, column);
		if (cursor.isBefore(root.getBegin().get()) || cursor.isAfter(root.getEnd().get()))
			bounds = false;
		// Iterate over children, return non-null child
		for (Node child : root.getChildNodes()) {
			Node ret = getNodeAtLocation(line, column, child);
			if (ret != null)
				return ret;
		}
		// If we're not in bounds and none of our children are THEN we assume this node is bad.
		if (!bounds)
			return null;
		return root;
	}
}
