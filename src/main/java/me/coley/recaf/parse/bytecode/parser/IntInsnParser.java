package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.util.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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
				throw new ASTParseException(lineNo, "Not enough parameters");
			int start = line.indexOf(trim[0]);
			// op
			OpcodeParser opParser = new OpcodeParser();
			opParser.setOffset(line.indexOf(trim[0]));
			OpcodeAST op = opParser.visit(lineNo, trim[0]);
			// TODO: For NEWARRAY, using types instead of magic number values would be intuitive
			String valueStr = trim[1];
			int valueStrStart = line.indexOf(valueStr);
			NumberAST num = null;
			if (op.getOpcode() == Opcodes.NEWARRAY) {
				// Type to value
				DescParser descParser = new DescParser();
				descParser.setOffset(line.indexOf(valueStr));
				DescAST desc = descParser.visit(lineNo, valueStr);
				if (!TypeUtil.isPrimitiveDesc(desc.getDesc())) {
					throw new ASTParseException(lineNo, "Expected primitive descriptor for NEWARRAY");
				}
				num = new NumberAST(lineNo, valueStrStart,
						TypeUtil.typeToNewArrayArg(Type.getType(desc.getDesc())));
			} else {
				// Value
				IntParser numParser = new IntParser();
				numParser.setOffset(valueStrStart);
				num = numParser.visit(lineNo, valueStr);
			}
			return new IntInsnAST(lineNo, start, op, num);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for int instruction");
		}
	}
}
