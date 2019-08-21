package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.NameParser;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.List;

/**
 * Label type instruction parser.
 *
 * @author Matt
 */
public class LabelVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public LabelVisitor(AssemblyVisitor asm) {
		super(asm);
		addSection(new NameParser(NameParser.VarType.VARIABLE));
	}

	@Override
	public void visitPre(String text) throws LineParseException {
		List<Object> args = parse(text);
		String name = (String) args.get(1);
		asm.getLabels().register(name);
	}

	@Override
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		String name = (String) args.get(1);
		LabelNode label = asm.getLabels().get(name);
		if (label == null)
			throw new LineParseException(text, "No label by the given name: " + name);
		asm.appendInsn(label);
	}
}
