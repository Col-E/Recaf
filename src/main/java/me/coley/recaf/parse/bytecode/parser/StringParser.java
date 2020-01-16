package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ast.StringAST;

/**
 * {@link StringAST} parser.
 *
 * @author Matt
 */
public class StringParser extends AbstractParser<StringAST> {
	@Override
	public StringAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			if(!trim.matches("\".*\""))
				throw new ASTParseException(lineNo, "Invalid string: " + trim);
			int start = line.indexOf(trim);
			return new StringAST(lineNo, getOffset() + start, trim.substring(1, trim.length() - 1));
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for number");
		}
	}
}
