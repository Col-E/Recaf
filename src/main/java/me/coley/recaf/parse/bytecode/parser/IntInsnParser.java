package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ast.*;

/**
 * {@link IntInsnAST} parser.
 *
 * @author Matt
 */
public class IntInsnParser extends AbstractParser<IntInsnAST> {
	@Override
	public IntInsnAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String[] trim = line.trim().split("\\s+");
			if (trim.length < 2)
				throw new ASTParseException(lineNo, "Not enough paramters");
			int start = line.indexOf(trim[0]);
			// op
			OpcodeParser opParser = new OpcodeParser();
			opParser.setOffset(line.indexOf(trim[0]));
			OpcodeAST op = opParser.visit(lineNo, trim[0]);
			// value
			IntParser numParser = new IntParser();
			numParser.setOffset(line.indexOf(trim[1]));
			NumberAST num = numParser.visit(lineNo, trim[1]);
			return new IntInsnAST(lineNo, start, op, num);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for int instruction");
		}
	}
}
