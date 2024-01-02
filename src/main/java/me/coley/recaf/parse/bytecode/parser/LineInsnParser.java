package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;

import java.util.Collections;
import java.util.List;

/**
 * {@link LineInsnAST} parser.
 *
 * @author Matt
 */
public class LineInsnParser extends AbstractParser<LineInsnAST> {
	@Override
	public LineInsnAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String[] trim = line.trim().split("\\s+");
			if (trim.length < 3)
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
			// line
			IntParser numParser = new IntParser();
			numParser.setOffset(line.indexOf(trim[2]));
			NumberAST lineNum = numParser.visit(lineNo, trim[2]);
			return new LineInsnAST(lineNo, start, op, label, lineNum);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for line-number instruction");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		// LINE label number
		if (text.contains(" ")) {
			String[] parts = text.split("\\s+");
			// Only complete if we're on the label portion
			if (parts.length == 2)
				return new NameParser(this).suggest(lastParse, parts[parts.length - 1]);
		}
		return Collections.emptyList();
	}
}
