package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.NumericParser;
import org.objectweb.asm.tree.IntInsnNode;

import java.util.List;

/**
 * Integer type instruction parser.
 *
 * @author Matt
 */
public class IntVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public IntVisitor(AssemblyVisitor asm) {
		super(asm);
		addSection(new NumericParser("value"));
	}

	@Override
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		int opcode = (int) args.get(0);
		Number value = (Number) args.get(1);
		if (!(value instanceof Integer)){
			String type = value.getClass().getSimpleName();
			throw new LineParseException(text, "Expected integer, but found: " + type);
		}
		asm.appendInsn(new IntInsnNode(opcode, value.intValue()));
	}
}
