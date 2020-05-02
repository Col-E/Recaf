package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ast.*;

/**
 * {@link InsnAST} parser.
 *
 * @author Matt
 */
public class InsnParser extends AbstractParser<InsnAST> {
	@Override
	public InsnAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			OpcodeParser opParser = new OpcodeParser();
			opParser.setOffset(line.indexOf(trim));
			OpcodeAST op = opParser.visit(lineNo, trim);
			int start = line.indexOf(trim);
			return new InsnAST(lineNo, start, op);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for basic instruction");
		}
	}
}
