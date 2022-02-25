package me.coley.recaf.parse.source;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import me.coley.recaf.util.JavaParserRecovery;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.JavaResource;
import me.coley.recaf.workspace.Workspace;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Source code wrapper.
 *
 * @author Matt
 */
public class SourceCode {
	/**
	 * Simple names belonging to the <i>"java.lang"</i> package.
	 */
	public static final String[] LANG_PACKAGE_NAMES;
	private static final String DEFAULT_PACKAGE = "";
	private final JavaResource resource;
	private final String code;
	private final List<String> lines;
	private CompilationUnit unit;
	// JavaParser values. Lazily instantiated.
	private List<String> explicitImports;
	private List<String> impliedImports;
	private String packageName;
	private String simpleName;
	private String internalName;

	/**
	 * @param resource
	 * 		Resource this source is attached to.
	 * @param code
	 * 		Full source code text.
	 */
	public SourceCode(JavaResource resource, String code) {
		this.resource = resource;
		this.code = code;
		this.lines = Arrays.asList(StringUtil.splitNewline(code));
	}

	/**
	 * Analyze the source code minimally.
	 *
	 * @return Parse result of class.
	 *
	 * @throws SourceCodeException
	 * 		Thrown if the source code could not be parsed.
	 */
	public ParseResult<CompilationUnit> analyze() throws SourceCodeException {
		ParseResult<CompilationUnit> result = analyze0(code, new JavaParser());
		if(!result.getProblems().isEmpty())
			throw new SourceCodeException(result);
		return result;
	}

	/**
	 * Analyze the source code.
	 *
	 * @param workspace
	 * 		Workspace to use for assistance in type resolving.
	 *
	 * @return Parse result of class.
	 *
	 * @throws SourceCodeException
	 * 		Thrown if the source code could not be parsed.
	 */
	public ParseResult<CompilationUnit> analyze(Workspace workspace) throws SourceCodeException {
		ParseResult<CompilationUnit> result = analyze0(code, new JavaParser(workspace.getSourceParseConfig()));
		if(!result.getProblems().isEmpty())
			throw new SourceCodeException(result);
		return result;
	}

	/**
	 * Analyze the source code with known problems filtered out.
	 *
	 * @param workspace
	 * 		Workspace to use for assistance in type resolving.
	 *
	 * @param knownProblems
	 *      Known problems that was arisen in parsing
	 *
	 * @return Parse result of class.
	 */
	public ParseResult<CompilationUnit> analyzeFiltered(Workspace workspace, Collection<Problem> knownProblems) {
		String cleanedCode = JavaParserRecovery.filterDecompiledCode(code, knownProblems);
		return analyze0(cleanedCode, new JavaParser(workspace.getSourceParseConfig()));
	}

	/**
	 * Analyze the source code using the specific parser.
	 *
	 * @param parser
	 *      the parser instance.
	 *
	 * @return
	 *      Parse result of class.
	 */
	private ParseResult<CompilationUnit> analyze0(String code, JavaParser parser) {
		ParseResult<CompilationUnit> result = parser.parse(code);
		if(result.getResult().isPresent())
			this.unit = result.getResult().get();
		return result;
	}

	/**
	 * Returns the AST node at the given position. Returns the deepest node in the AST at the point.
	 *
	 * @param line
	 * 		Cursor line.
	 * @param column
	 * 		Cursor column.
	 *
	 * @return JavaParser AST node at the given position in the source code.
	 */
	public Node getVerboseNodeAt(int line, int column) {
		return getNodeAt(line, column, unit.findRootNode(), node -> {
			// Verify the node range can be accessed
			if (!node.getBegin().isPresent() || !node.getEnd().isPresent())
				return false;
			// Should be fine
			return true;
		});
	}

	/**
	 * Returns the AST node at the given position.
	 * The child-most node may not be returned if the parent is better suited for contextual
	 * purposes.
	 *
	 * @param line
	 * 		Cursor line.
	 * @param column
	 * 		Cursor column.
	 *
	 * @return JavaParser AST node at the given position in the source code.
	 */
	public Node getNodeAt(int line, int column) {
		return getNodeAt(line, column, unit.findRootNode(), node -> {
			// We want to know more about this type, don't resolve down to the lowest AST
			// type... the parent has more data and is essentially just a wrapper around SimpleName.
			if (node instanceof SimpleName)
				return false;
			// Verify the node range can be accessed
			if (!node.getBegin().isPresent() || !node.getEnd().isPresent())
				return false;
			// Same as above, we want to return the node with actual context.
			return !(node instanceof NameExpr);
			// Should be fine
		});
	}

	private Node getNodeAt(int line, int column, Node root, Predicate<Node> filter) {
		if (!filter.test(root))
			return null;
		// Check cursor is in bounds
		// We won't instantly return null because the root range may be SMALLER than
		// the range of children. This is really stupid IMO but thats how JavaParser is...
		boolean bounds = true;
		Position cursor = new Position(line, column);
		if (cursor.isBefore(root.getBegin().get()) || cursor.isAfter(root.getEnd().get()))
			bounds = false;
		// Iterate over children, return non-null child
		for (Node child : root.getChildNodes()) {
			Node ret = getNodeAt(line, column, child, filter);
			if (ret != null)
				return ret;
		}
		// If we're not in bounds and none of our children are THEN we assume this node is bad.
		if (!bounds)
			return null;
		// In bounds so we're good!
		return root;
	}

	/**
	 * @return Class package in standard format <i>(Not internal, using ".")</i>
	 */
	public String getPackage() {
		if (packageName != null)
			return packageName;
		// fetch package
		return packageName = unit.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse(DEFAULT_PACKAGE);
	}

	/**
	 * @return List of classes imported. Wildcards are mapped to the entire package.
	 */
	public List<String> getImports() {
		if (explicitImports != null)
			return explicitImports;
		// compute imports
		return explicitImports = unit.getImports().stream().flatMap(imp -> {
			// Ignore static imports
			if (imp.isStatic())
				return Stream.empty();
			// Check wildcard import
			if (imp.isAsterisk()) {
				String pkg = imp.getNameAsString();
				return resource.getClasses().keySet().stream()
						.filter(name -> {
							int index = name.lastIndexOf('/');
							if (index == -1) return false;
							String tmpPackageName = name.substring(0, index);
							return tmpPackageName.equals(pkg);
						});
			}
			// Single class import
			return Stream.of(imp.getNameAsString().replace('.', '/'));
		}).collect(Collectors.toList());
	}

	/**
	 * @return List of all classes imported. This includes the {@link #getImports() explicit
	 * imports} and the implied classes from the current and "java.lang" packages.
	 */
	public List<String> getAllImports() {
		if (impliedImports != null)
			return impliedImports;
		// Get stream of classes in the same package
		String pkg = getPackage();
		Stream<String> pkgStream;
		if (pkg.equals(DEFAULT_PACKAGE))
			pkgStream = resource.getClasses().keySet().stream().filter(name ->!name.contains("/"));
		else
			pkgStream = resource.getClasses().keySet().stream().filter(name -> {
				if (!name.contains("/"))
					return false;
				String tmpPackageName = name.substring(0, name.lastIndexOf("/"));
				return tmpPackageName.equals(pkg);
			});
		pkgStream = Stream.concat(pkgStream, Stream.of(LANG_PACKAGE_NAMES).map(n -> "java/lang/" + n));
		// Combine with explicit
		return impliedImports = Stream.concat(getImports().stream(), pkgStream)
				.collect(Collectors.toList());
	}

	/**
	 * @return Class name.
	 */
	public String getName() {
		if (simpleName != null)
			return simpleName;
		// fetch declared name (Should be same as source file name)
		TypeDeclaration<?> type = unit.getType(0);
		if(type != null)
			return simpleName = type.getNameAsString();
		throw new IllegalStateException("Failed to fetch type from source file: " + code);
	}

	/**
	 * @return Internal class name representation.
	 */
	public String getInternalName() {
		if (internalName != null)
			return internalName;
		// compute internal name
		if(getPackage().equals(DEFAULT_PACKAGE))
			return internalName = getName();
		return internalName = (getPackage() + "." + getName()).replace(".", "/");
	}

	/**
	 * @param line
	 * 		The source line to target.
	 * @param context
	 * 		The number of lines before and after the targeted line to include.
	 *
	 * @return Source from lines (line - context) to (line + context).
	 */
	public String getSurrounding(int line, int context) {
		// Offset so we're 0-based
		line--;
		//
		int min = Math.max(0, line - context);
		int max = Math.min(lines.size() - 1, line + context);
		StringBuilder sb = new StringBuilder();
		for (int i = min; i <= max; i++) {
			sb.append(lines.get(i));
			if (i < max)
				sb.append('\n');
		}
		return sb.toString();
	}

	/**
	 * @return Abstract syntax tree representation of source code.
	 */
	public CompilationUnit getUnit() {
		return unit;
	}

	/**
	 * @return Full source code text.
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @return Full source code split by newlines.
	 */
	public List<String> getLines() {
		return lines;
	}

	static {
		// I'm hiding this behemoth down here.
		// - Don't touch it, Checkstyle will complain
		//
		// There's no clean way to look up things in a package.
		// "java.lang" won't change often so.... this is fine
		LANG_PACKAGE_NAMES = new String[]{"AbstractMethodError", "Annotation", "Appendable",
			"ArithmeticException", "ArrayIndexOutOfBoundsException", "ArrayStoreException",
			"AssertionError", "AutoCloseable", "Boolean", "BootstrapMethodError", "Byte",
			"Character", "CharSequence", "Class", "ClassCastException", "ClassCircularityError",
			"ClassFormatError", "ClassLoader", "ClassNotFoundException", "ClassValue",
			"Cloneable", "CloneNotSupportedException", "Comparable", "Compiler", "Deprecated",
			"Double", "Enum", "EnumConstantNotPresentException", "Error", "Exception",
			"ExceptionInInitializerError", "Float", "IllegalAccessError",
			"IllegalAccessException", "IllegalArgumentException",
			"IllegalMonitorStateException", "IllegalStateException",
			"IllegalThreadStateException", "IncompatibleClassChangeError",
			"IndexOutOfBoundsException", "InheritableThreadLocal", "InstantiationError",
			"InstantiationException", "Integer", "Interface", "InternalError",
			"InterruptedException", "Iterable", "LinkageError", "Long", "Math",
			"NegativeArraySizeException", "NoClassDefFoundError", "NoSuchFieldError",
			"NoSuchFieldException", "NoSuchMethodError", "NoSuchMethodException",
			"NullPointerException", "Number", "NumberFormatException", "Object",
			"OutOfMemoryError", "Override", "Package", "Process", "ProcessBuilder", "Readable",
			"ReflectiveOperationException", "Runnable", "Runtime", "RuntimeException",
			"RuntimePermission", "SafeVarargs", "SecurityException", "SecurityManager", "Short",
			"StackOverflowError", "StackTraceElement", "StrictMath", "String", "StringBuffer",
			"StringBuilder", "StringIndexOutOfBoundsException", "SuppressWarnings", "System",
			"Thread", "ThreadDeath", "ThreadGroup", "ThreadLocal", "Throwable",
			"TypeNotPresentException", "UnknownError", "UnsatisfiedLinkError",
			"UnsupportedClassVersionError", "UnsupportedOperationException", "VerifyError",
			"VirtualMachineError", "Void"};
	}
}
