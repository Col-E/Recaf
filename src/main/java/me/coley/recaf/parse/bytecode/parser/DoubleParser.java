package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ast.NumberAST;

/**
 * {@link NumberAST} parser for doubles.
 *
 * @author Matt
 */
public class DoubleParser extends AbstractParser<NumberAST> {
	@Override
	public NumberAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			if(!trim.matches("-?[.\\d]+[Dd]?"))
				throw new ASTParseException(lineNo, "Invalid double: " + trim);
			int start = line.indexOf(trim);
			return new NumberAST(lineNo, getOffset() + start, Double.valueOf(trim));
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for number");
		}
	}
}
