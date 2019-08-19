package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.*;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.List;

/**
 * Method type instruction parser.
 *
 * @author Matt
 */
public class MethodVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public MethodVisitor(AssemblyVisitor asm) {
		super(asm);
		addSection(new InternalNameParser("owner"));
		addSection(new NameParser(NameParser.VarType.METHOD));
		addSection(new DescriptorParser(DescriptorParser.DescType.METHOD));
	}

	@Override
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		int opcode = (int) args.get(0);
		String owner = (String) args.get(1);
		String name = (String) args.get(2);
		String desc = (String) args.get(3);
		asm.appendInsn(new MethodInsnNode(opcode, owner, name, desc));
	}
}
