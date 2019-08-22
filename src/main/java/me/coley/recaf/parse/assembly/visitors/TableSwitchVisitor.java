package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.ListParser;
import org.objectweb.asm.tree.*;

import java.util.List;

/**
 * Table switch type instruction parser.
 *
 * @author Matt
 */
public class TableSwitchVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public TableSwitchVisitor(AssemblyVisitor asm) {
		super(asm);
		// Format: range[0-2] offsets[A, B, C] default[D]
		addSection(new ListParser("range"));
		addSection(new ListParser("offsets"));
		addSection(new ListParser("default"));
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		// Decode range
		String range = ((List<String>) args.get(1)).get(0);
		if (!range.matches("\\d+-\\d+"))
			throw new LineParseException(text, "Range format invalid, requires [min-max]");
		int split = range.indexOf("-", 1);
		int min = Integer.parseInt(range.substring(0, split));
		int max = Integer.parseInt(range.substring(split + 1));
		// Decode labels
		List<String> offsets = (List<String>) args.get(2);
		if (max - min != offsets.size() - 1)
			throw new LineParseException(text,
					"Range difference size does not match number of given offsets labels");
		LabelNode[] labels = new LabelNode[offsets.size()];
		for (int i = 0; i < labels.length; i++) {
			String off = offsets.get(i);
			LabelNode lbl = asm.getLabels().get(off);
			if (lbl == null)
				throw new LineParseException(text, "No label by the given name: " + off);
			labels[i] = lbl;
		}
		// Decode default
		String dflt = ((List<String>) args.get(3)).get(0);
		LabelNode dfltLbl = asm.getLabels().get(dflt);
		if (dfltLbl == null)
			throw new LineParseException(text, "No label by the given name: " + dflt);
		//
		asm.appendInsn(new TableSwitchInsnNode(min, max, dfltLbl, labels));
	}
}
