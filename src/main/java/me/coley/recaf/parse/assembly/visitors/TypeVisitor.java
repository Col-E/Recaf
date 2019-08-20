package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.*;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.List;

/**
 * Type type instruction parser.
 *
 * @author Matt
 */
public class TypeVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public TypeVisitor(AssemblyVisitor asm) {
		super(asm);
		addSection(new InternalNameParser("type"));
	}

	@Override
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		int opcode = (int) args.get(0);
		String type = (String) args.get(1);
		asm.appendInsn(new TypeInsnNode(opcode, type));
	}
}
