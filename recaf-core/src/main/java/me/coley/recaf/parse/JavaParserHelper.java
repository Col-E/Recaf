package me.coley.recaf.parse;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import me.coley.recaf.Controller;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.code.MethodInfo;

import java.util.List;
import java.util.Optional;

/**
 * Utility for working with JavaParser and pulling contextual information from Java source code.
 *
 * @author Matt Coley
 */
public class JavaParserHelper {
	private final WorkspaceTypeSolver typeSolver;
	private final JavaSymbolSolver symbolSolver;
	private final JavaParser parser;

	private JavaParserHelper(WorkspaceTypeSolver typeSolver) {
		this.typeSolver = typeSolver;
		symbolSolver = new JavaSymbolSolver(typeSolver);
		parser = new JavaParser(new ParserConfiguration()
				.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_16)
				.setSymbolResolver(symbolSolver));
	}

	/**
	 * @param controller
	 * 		Recaf controller, used to pull a {@link WorkspaceTypeSolver}.
	 *
	 * @return Instance of JavaParser helper utility.
	 */
	public static JavaParserHelper create(Controller controller) {
		return create(controller.getServices().getTypeSolver());
	}

	/**
	 * @param typeSolver
	 * 		Type solver for JavaParser integration with types recognized by Recaf.
	 *
	 * @return Instance of JavaParser helper utility.
	 */
	public static JavaParserHelper create(WorkspaceTypeSolver typeSolver) {
		return new JavaParserHelper(typeSolver);
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
		ParseResult<CompilationUnit> result = parser.parse(code);
		List<Problem> problems = result.getProblems();
		if (result.getResult().isPresent()) {
			// There is a compilation unit, but is it usable?
			CompilationUnit unit = result.getResult().get();
			if (tryRecover && isInvalidCompilationUnit(unit)) {
				// Its not usable at all, attempt to recover
				return new JavaParserRecovery(this).parseClassWithRecovery(code, problems);
			} else {
				// The unit is usable, but may contain some localized problems.
				// Check if we want to try to resolve any reported problems.
				if (tryRecover && !result.getProblems().isEmpty()) {
					return new JavaParserRecovery(this).parseClassWithRecovery(code, problems);
				}
				// Update unit and roll with whatever we got.
				updateUnit(unit);
			}
		} else if (tryRecover) {
			// No unit in result, attempt to recover
			return new JavaParserRecovery(this).parseClassWithRecovery(code, problems);
		}
		return result;
	}

	/**
	 * Adds some missing meta-data to compilation units.
	 *
	 * @param unit
	 * 		Unit to update metadata of.
	 */
	private void updateUnit(CompilationUnit unit) {
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
		if (unit != null) {
			Node node = getNodeAtLocation(line, column, unit);
			while (node != null && JavaParserResolving.isNodeResolvable(node)) {
				ItemInfo value = JavaParserResolving.of(typeSolver, node);
				if (value == null) {
					Optional<Node> parent = node.getParentNode();
					if (parent.isPresent()) {
						// Check if the parent item can be resolved
						node = parent.get();
						continue;
					} else {
						// No parent, end the loop
						break;
					}
				} else if (node instanceof SimpleName && value instanceof FieldInfo) {
					// In some obfuscated cases, a method name may be selected as an AST 'SimpleName'.
					// These typically resolve to fields of matching names.
					// If the parent of the node is a method of the same name, we probably meant to
					// yield the method info, not a field by the same name.
					FieldInfo fieldInfo = (FieldInfo) value;
					if (node.hasParentNode()) {
						ItemInfo info = JavaParserResolving.of(typeSolver, node.getParentNode().get());
						if (info instanceof MethodInfo) {
							MethodInfo methodInfo = (MethodInfo) info;
							if (methodInfo.getName().equals(fieldInfo.getName())) {
								value = info;
							}
						}
					}
				}
				return Optional.of(new ParseHitResult(value, node));
			}
			// Handle edge cases like package import names.
			ItemInfo value = JavaParserResolving.ofEdgeCases(typeSolver, node);
			if (value != null) {
				return Optional.of(new ParseHitResult(value, node));
			}
		}
		return Optional.empty();
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
				ItemInfo value = JavaParserResolving.of(typeSolver, node);
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
