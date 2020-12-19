package me.coley.recaf.parse.bytecode;

import me.coley.recaf.parse.bytecode.ast.RootAST;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;

/**
 * Common assembler.
 *
 * @param <T>
 * 		Output type.
 *
 * @author Matt
 */
public interface Assembler<T> {
	/**
	 * @param result
	 * 		AST parse result.
	 *
	 * @return Compiled value.
	 *
	 * @throws AssemblerException
	 * 		<ul>
	 * 						<li>When the given AST contains errors</li>
	 * 						<li>When the given AST is missing a definition</li>
	 * 						</ul>
	 */
	T compile(ParseResult<RootAST> result) throws AssemblerException;
}
