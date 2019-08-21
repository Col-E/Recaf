package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.NameParser;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.List;

/**
 * Variable type instruction parser.
 *
 * @author Matt
 */
public class VarVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public VarVisitor(AssemblyVisitor asm) {
		super(asm);
		addSection(new NameParser(NameParser.VarType.VARIABLE));
	}

	@Override
	public void visitPre(String text) throws LineParseException {
		List<Object> args = parse(text);
		int opcode = (int) args.get(0);
		String name = (String) args.get(1);
		asm.getVariables().register(name, opcode);
	}

	@Override
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		int opcode = (int) args.get(0);
		String name = (String) args.get(1);
		int index = asm.getVariables().getIndex(name);
		asm.appendInsn(new VarInsnNode(opcode, index));
	}
}
