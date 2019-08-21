package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.NameParser;
import me.coley.recaf.parse.assembly.parsers.NumericParser;
import org.objectweb.asm.tree.IincInsnNode;

import java.util.List;

import static org.objectweb.asm.Opcodes.IINC;

/**
 * Iinc type instruction parser.
 *
 * @author Matt
 */
public class IincVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public IincVisitor(AssemblyVisitor asm) {
		super(asm);
		addSection(new NameParser(NameParser.VarType.VARIABLE));
		addSection(new NumericParser("incr"));
	}

	@Override
	public void visitPre(String text) throws LineParseException {
		List<Object> args = parse(text);
		String name = (String) args.get(1);
		asm.getVariables().register(name, IINC);
	}

	@Override
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		String name = (String) args.get(1);
		int index = asm.getVariables().getIndex(name);
		Number value = (Number) args.get(2);
		if (!(value instanceof Integer)){
			String type = value.getClass().getSimpleName();
			throw new LineParseException(text, "Expected integer, but found: " + type);
		}
		asm.appendInsn(new IincInsnNode(index, value.intValue()));
	}
}
