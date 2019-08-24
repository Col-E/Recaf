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
		// Check for descriptor types like "[Ljava/lang/String;" before checking for internal names.
		addSection(new MultiParser("type",
				new DescriptorParser("type", DescriptorParser.DescType.FIELD),
				new InternalNameParser("type")));
	}

	@Override
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		int opcode = (int) args.get(0);
		String type = (String) args.get(1);
		asm.appendInsn(new TypeInsnNode(opcode, type));
	}
}
