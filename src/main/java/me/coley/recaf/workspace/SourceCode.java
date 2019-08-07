package me.coley.recaf.workspace;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import me.coley.recaf.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Source code wrapper.
 *
 * @author Matt
 */
public class SourceCode {
	private static final String DEFAULT_PACKAGE = "";
	private final JavaResource resource;
	private final CompilationUnit unit;
	private final String code;
	private final List<String> lines;
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
	 *
	 * @throws SourceCodeException
	 * 		Thrown if the source code could not be parsed.
	 */
	public SourceCode(JavaResource resource, String code) throws SourceCodeException {
		this.resource = resource;
		this.code = code;
		this.lines = Arrays.asList(StringUtil.splitNewline(code));
		ParseResult<CompilationUnit> unit = new JavaParser(new ParserConfiguration()).parse(code);
		if(!unit.isSuccessful())
			throw new SourceCodeException(unit);
		this.unit = unit.getResult().get();
	}

	/**
	 * @return Class package in standard format <i>(Not internal, using ".")</i>
	 */
	public String getPackage() {
		if (packageName != null)
			return packageName;
		// fetch package
		if(unit.getPackageDeclaration().isPresent())
			return packageName = unit.getPackageDeclaration().get().getNameAsString();
		return packageName = DEFAULT_PACKAGE;
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
							if (!name.contains("/"))
								return false;
							String tmpPackageName = name.substring(0, name.lastIndexOf("/"));
							return tmpPackageName.equals(pkg);
						});
			}
			// Single class import
			return Stream.of(imp.getNameAsString().replace(".", "/"));
		}).collect(Collectors.toList());
	}

	/**
	 * @return List of all classes imported. This includes the {@link #getImports() explicit
	 * imports} and the implied classes from the current package.
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
}
