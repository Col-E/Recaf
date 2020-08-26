package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;

/**
 * Represents compilable AST element.
 *
 * @author xxDark
 */
public interface Compilable {

    /**
     * Compiles this element.
     *
     * @param compilation
     * 		Compilation context.
     *
     * @throws AssemblerException
     * 		When compilation has failed.
     */
    void compile(MethodCompilation compilation) throws AssemblerException;
}
