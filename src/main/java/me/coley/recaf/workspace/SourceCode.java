package me.coley.recaf.workspace;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;

/**
 * Source code wrapper.
 *
 * @author Matt
 */
public class SourceCode {
	private static final String DEFAULT_PACKAGE = "";
	private final String code;
	private final CompilationUnit unit;

	/**
	 * @param code
	 * 		Full source code text.
	 *
	 * @throws SourceCodeException
	 * 		Thrown if the source code could not be parsed.
	 */
	public SourceCode(String code) throws SourceCodeException {
		this.code = code;
		ParseResult<CompilationUnit> unit = new JavaParser(new ParserConfiguration()).parse(code);
		if(!unit.isSuccessful())
			throw new SourceCodeException(unit);
		this.unit = unit.getResult().get();
	}

	/**
	 * @return Class package in standard format <i>(Not internal, using ".")</i>
	 */
	public String getPackage() {
		if(unit.getPackageDeclaration().isPresent()) {
			return unit.getPackageDeclaration().get().getNameAsString();
		}
		return DEFAULT_PACKAGE;
	}

	/**
	 * @return Class name.
	 */
	public String getName() {
		TypeDeclaration<?> type = unit.getType(0);
		if(type != null) {
			return type.getNameAsString();
		}
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
}
