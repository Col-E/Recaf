package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ast.*;

/**
 * {@link LabelAST} parser.
 *
 * @author Matt
 */
public class LabelParser extends AbstractParser<LabelAST> {
	@Override
	public LabelAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			String name = trim.substring(0, trim.indexOf(':'));
			NameParser nameParser = new NameParser(this);
			nameParser.setOffset(line.indexOf(name));
			NameAST ast = nameParser.visit(lineNo, name);
			int start = line.indexOf(trim);
			return new LabelAST(lineNo, start, ast);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for label, expected colon(:) at end");
		}
	}
}
