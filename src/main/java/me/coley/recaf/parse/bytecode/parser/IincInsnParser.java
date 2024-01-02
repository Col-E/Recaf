package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;

import java.util.Collections;
import java.util.List;

/**
 * {@link IincInsnAST} parser.
 *
 * @author Matt
 */
public class IincInsnParser extends AbstractParser<IincInsnAST> {
	@Override
	public IincInsnAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String[] trim = line.trim().split("\\s+");
			if (trim.length < 3)
				throw new ASTParseException(lineNo, "Not enough parameters");
			int start = line.indexOf(trim[0]);
			// op
			OpcodeParser opParser = new OpcodeParser();
			opParser.setOffset(line.indexOf(trim[0]));
			OpcodeAST op = opParser.visit(lineNo, trim[0]);
			// variable
			NameParser nameParser = new NameParser(this);
			nameParser.setOffset(line.indexOf(trim[1]));
			NameAST variable = nameParser.visit(lineNo, trim[1]);
			// incr
			IntParser numParser = new IntParser();
			numParser.setOffset(line.indexOf(trim[2]));
			NumberAST incr = numParser.visit(lineNo, trim[2]);
			return new IincInsnAST(lineNo, start, op, variable, incr);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for increment instruction");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		try {
			NameParser nameParser = new NameParser(this);
			String[] parts = text.trim().split("\\s+");
			// last word is the 'variable' portion
			if(parts.length == 2)
				return nameParser.suggest(lastParse, parts[parts.length - 1]);
		} catch(Exception ex) { /* ignored */ }
		return Collections.emptyList();
	}
}
