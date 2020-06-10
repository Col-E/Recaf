package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ast.NumberAST;

/**
 * {@link NumberAST} parser for longs.
 *
 * @author Matt
 */
public class LongParser extends AbstractParser<NumberAST> {
	@Override
	public NumberAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			if(!trim.matches("-?\\d+[LlJj]?"))
				if (!trim.matches("-?[\\d.]+(?:[eE]-?\\d+)?[LlJj]?"))
					throw new ASTParseException(lineNo, "Invalid long: " + trim);
			char last = trim.charAt(trim.length() - 1);
			if (!(last > '0' && last < '9'))
				trim = trim.substring(0, trim.length() - 1);
			int start = line.indexOf(trim);
			return new NumberAST(lineNo, getOffset() + start, Long.valueOf(trim));
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for number");
		}
	}
}
