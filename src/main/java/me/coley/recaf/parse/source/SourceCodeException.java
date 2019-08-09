package me.coley.recaf.parse.source;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;

/**
 * Exception thrown for when the JavaParser api fails to parse a source file.
 *
 * @author Matt
 */
public class SourceCodeException extends Exception {
	private final ParseResult<CompilationUnit> result;

	/**
	 * @param result Result returned as a parse failure.
	 */
	public SourceCodeException(ParseResult<CompilationUnit> result) {
		this.result = result;
	}

	/**
	 * @return Failing parse result.
	 */
	public ParseResult<CompilationUnit> getResult() {
		return result;
	}
}
