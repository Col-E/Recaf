package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.InternalNameParser;
import me.coley.recaf.parse.assembly.parsers.NumericParser;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.List;

/**
 * MultiANewArray type instruction parser.
 *
 * @author Matt
 */
public class MultiANewArrayVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public MultiANewArrayVisitor(AssemblyVisitor asm) {
		super(asm);
		addSection(new InternalNameParser("type"));
		addSection(new NumericParser("dimensions"));
	}

	@Override
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		String type = (String) args.get(1);
		Number dimensions = (Number) args.get(2);
		if (!(dimensions instanceof Integer)){
			String dimType = dimensions.getClass().getSimpleName();
			throw new LineParseException(text, "Expected integer for dimension size, but found: " + dimType);
		}
		int dims = dimensions.intValue();
		if (dims <= 0)
			throw new LineParseException(text, "Expected dimension size greater than 0 but found: " + dims);
		asm.appendInsn(new MultiANewArrayInsnNode(type, dims));
	}
}
