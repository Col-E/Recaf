package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LdcInsnNode;

import java.util.List;

/**
 * Constant type instruction parser.
 *
 * @author Matt
 */
public class LdcVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public LdcVisitor(AssemblyVisitor asm) {
		super(asm);
		addSection(new MultiParser("value",
				new NumericParser("value"),
				new StringParser(),
				new DescriptorParser("value", DescriptorParser.DescType.FIELD)));
	}

	@Override
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		Object value = args.get(1);
		if (value instanceof String) {
			String valueStr =  (String) value;
			// Either a string or type
			// - Strings wrapped in "
			// - Otherwise should be a type
			if (text.contains("\"")) {
				// Escape common sequences
				valueStr = valueStr.replace("\\r", "\r");
				valueStr = valueStr.replace("\\n", "\n");
				valueStr = valueStr.replace("\\t", "\t");
				value = valueStr;
			} else {
				value = Type.getType(valueStr);
			}
		}
		asm.appendInsn(new LdcInsnNode(value));
	}
}
