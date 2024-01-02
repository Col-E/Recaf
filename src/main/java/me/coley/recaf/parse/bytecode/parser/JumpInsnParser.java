package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;

import java.util.Collections;
import java.util.List;

/**
 * {@link JumpInsnAST} parser.
 *
 * @author Matt
 */
public class JumpInsnParser extends AbstractParser<JumpInsnAST> {
	@Override
	public JumpInsnAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String[] trim = line.trim().split("\\s+");
			if (trim.length < 2)
				throw new ASTParseException(lineNo, "Not enough parameters");
			int start = line.indexOf(trim[0]);
			// op
			OpcodeParser opParser = new OpcodeParser();
			opParser.setOffset(line.indexOf(trim[0]));
			OpcodeAST op = opParser.visit(lineNo, trim[0]);
			// label
			NameParser nameParser = new NameParser(this);
			nameParser.setOffset(line.indexOf(trim[1]));
			NameAST label = nameParser.visit(lineNo, trim[1]);
			return new JumpInsnAST(lineNo, start, op, label);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for var instruction");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		if (text.contains(" ")) {
			String[] parts = text.split("\\s+");
			return new NameParser(this).suggest(lastParse, parts[parts.length - 1]);
		}
		return Collections.emptyList();
	}
}
