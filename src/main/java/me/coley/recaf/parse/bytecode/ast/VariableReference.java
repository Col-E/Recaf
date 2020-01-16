package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.AssemblerException;
import me.coley.recaf.parse.bytecode.Variables;

/**
 * Common to AST that reference variables.
 *
 * @author Matt
 */
public interface VariableReference {
	/**
	 * @return Variable name AST.
	 */
	NameAST getVariableName();

	/**
	 * @param variables
	 * 		Variable name-to-index lookup.
	 *
	 * @return Index of {@link #getVariableName()}.
	 *
	 * @throws AssemblerException
	 * 		Variable failed index lookup.
	 */
	default int getVariableIndex(Variables variables) throws AssemblerException {
		try {
			return variables.getIndex(getVariableName().getName());
		} catch(AssemblerException ex) {
			// Rethrow with line number
			int line = -1;
			if (this instanceof AST)
				line = ((AST) this).getLine();
			throw new AssemblerException(ex.getMessage(), line);
		}
	}
}
