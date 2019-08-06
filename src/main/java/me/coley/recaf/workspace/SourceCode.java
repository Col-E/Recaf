package me.coley.recaf.workspace;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import me.coley.recaf.util.StringUtil;

import java.util.Arrays;
import java.util.List;

/**
 * Source code wrapper.
 *
 * @author Matt
 */
public class SourceCode {
	private static final String DEFAULT_PACKAGE = "";
	private final CompilationUnit unit;
	private final String code;
	private final List<String> lines;

	/**
	 * @param code
	 * 		Full source code text.
	 *
	 * @throws SourceCodeException
	 * 		Thrown if the source code could not be parsed.
	 */
	public SourceCode(String code) throws SourceCodeException {
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
		if(unit.getPackageDeclaration().isPresent())
			return unit.getPackageDeclaration().get().getNameAsString();
		return DEFAULT_PACKAGE;
	}

	/**
	 * @return Class name.
	 */
	public String getName() {
		TypeDeclaration<?> type = unit.getType(0);
		if(type != null)
			return type.getNameAsString();
		throw new IllegalStateException("Failed to fetch type from source file: " + code);
	}

	/**
	 * @return Internal class name representation.
	 */
	public String getInternalName() {
		if(getPackage().equals(DEFAULT_PACKAGE))
			return getName();
		return (getPackage() + "." + getName()).replace(".", "/");
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
