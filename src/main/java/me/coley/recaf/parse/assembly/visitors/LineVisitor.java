package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.NameParser;
import me.coley.recaf.parse.assembly.parsers.NumericParser;
import org.objectweb.asm.tree.*;

import java.util.List;

import static org.objectweb.asm.Opcodes.IINC;

/**
 * Line type instruction parser.
 *
 * @author Matt
 */
public class LineVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public LineVisitor(AssemblyVisitor asm) {
		super(asm);
		addSection(new NumericParser("line"));
		addSection(new NameParser(NameParser.VarType.VARIABLE));
	}

	@Override
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		Number line = (Number) args.get(1);
		if (!(line instanceof Integer)){
			String type = line.getClass().getSimpleName();
			throw new LineParseException(text, "Expected integer, but found: " + type);
		} else if (line.intValue() < 0)
			throw new LineParseException(text, "Line number cannot be negative!");
		String name = (String) args.get(2);
		LabelNode label = asm.getLabels().get(name);
		if (label == null)
			throw new LineParseException(text, "No label by the given name: " + name);
		asm.appendInsn(new LineNumberNode(line.intValue(), label));
	}
}
