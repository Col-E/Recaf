package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import org.objectweb.asm.tree.InsnNode;

import java.util.List;

/**
 * Insn type instruction parser.
 *
 * @author Matt
 */
public class InsnVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public InsnVisitor(AssemblyVisitor asm) {
		super(asm);
	}

	@Override
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		int opcode = (int) args.get(0);
		asm.appendInsn(new InsnNode(opcode));
	}
}
