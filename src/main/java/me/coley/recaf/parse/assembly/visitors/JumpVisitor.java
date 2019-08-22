package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.NameParser;
import org.objectweb.asm.tree.*;

import java.util.List;

/**
 * Jump type instruction parser.
 *
 * @author Matt
 */
public class JumpVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public JumpVisitor(AssemblyVisitor asm) {
		super(asm);
		addSection(new NameParser(NameParser.VarType.VARIABLE));
	}

	@Override
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		int opcode = (int) args.get(0);
		String name = (String) args.get(1);
		LabelNode label = asm.getLabels().get(name);
		if (label == null)
			throw new LineParseException(text, "No label by the given name: " + name);
		asm.appendInsn(new JumpInsnNode(opcode, label));
	}
}
