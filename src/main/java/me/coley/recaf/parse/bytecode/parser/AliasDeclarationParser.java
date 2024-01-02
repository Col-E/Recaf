package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ast.*;

/**
 * {@link AliasAST} parser.
 *
 * @author Matt
 */
public class AliasDeclarationParser extends AbstractParser<AliasAST> {
	@Override
	public AliasAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String[] trim = line.trim().split("\\s+");
			if (trim.length < 2)
				throw new ASTParseException(lineNo, "Not enough parameters");
			int start = line.indexOf(trim[0]);
			// op
			OpcodeParser opParser = new OpcodeParser();
			opParser.setOffset(line.indexOf(trim[0]));
			OpcodeAST op = opParser.visit(lineNo, trim[0]);
			// name
			NameParser nameParser = new NameParser(this);
			nameParser.setOffset(line.indexOf(trim[1]));
			NameAST name = nameParser.visit(lineNo, trim[1]);
			// content
			StringParser stringParser = new StringParser();
			stringParser.setOffset(line.indexOf("\""));
			StringAST content = stringParser.visit(lineNo, line.substring(line.indexOf("\"")));
			return new AliasAST(lineNo, start, op, name, content);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for var instruction");
		}
	}
}
