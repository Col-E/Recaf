package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;

import java.util.Collections;
import java.util.List;

/**
 * {@link MultiArrayInsnAST} parser.
 *
 * @author Matt
 */
public class MultiArrayParser extends AbstractParser<MultiArrayInsnAST> {
	@Override
	public MultiArrayInsnAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String[] trim = line.trim().split("\\s+");
			if (trim.length < 2)
				throw new ASTParseException(lineNo, "Not enough parameters");
			int start = line.indexOf(trim[0]);
			// op
			OpcodeParser opParser = new OpcodeParser();
			opParser.setOffset(line.indexOf(trim[0]));
			OpcodeAST op = opParser.visit(lineNo, trim[0]);
			// desc
			DescParser descParser = new DescParser();
			descParser.setOffset(line.indexOf(trim[1]));
			DescAST type = descParser.visit(lineNo, trim[1]);
			// dims
			IntParser numParser = new IntParser();
			numParser.setOffset(line.indexOf(trim[2]));
			NumberAST dims = numParser.visit(lineNo, trim[2]);
			return new MultiArrayInsnAST(lineNo, start, op, type, dims);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for MultiANewArray instruction");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		// MULTI type number
		if (text.contains(" ")) {
			String[] parts = text.split("\\s+");
			// Only complete if we're on the type portion
			if (parts.length == 2)
				return new DescParser().suggest(lastParse, parts[parts.length - 1]);
		}
		return Collections.emptyList();
	}
}
