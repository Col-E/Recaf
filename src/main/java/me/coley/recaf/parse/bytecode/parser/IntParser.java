package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ast.*;

/**
 * {@link NumberAST} parser for integers.
 *
 * @author Matt
 */
public class IntParser extends AbstractParser<NumberAST> {
	@Override
	public NumberAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			if(!trim.matches("-?\\d+"))
				throw new ASTParseException(lineNo, "Invalid integer: " + trim);
			int start = line.indexOf(trim);
			return new NumberAST(lineNo, getOffset() + start, Integer.valueOf(trim));
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for number, value not a valid int");
		}
	}
}
