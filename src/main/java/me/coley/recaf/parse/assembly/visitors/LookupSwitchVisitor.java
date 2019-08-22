package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.parsers.ListParser;
import org.objectweb.asm.tree.*;

import java.util.List;

/**
 * Lookup switch type instruction parser.
 *
 * @author Matt
 */
public class LookupSwitchVisitor extends InstructionVisitor {
	/**
	 * @param asm
	 * 		Parent assembly visitor for feeding back parsed content.
	 */
	public LookupSwitchVisitor(AssemblyVisitor asm) {
		super(asm);
		// Format: mapping[0=A, 1=B, 2=C] default[D]
		addSection(new ListParser("mapping"));
		addSection(new ListParser("default"));
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visit(String text) throws LineParseException {
		List<Object> args = parse(text);
		// Decode lookup
		List<String> mappings = (List<String>) args.get(1);
		int[] keys = new int[mappings.size()];
		LabelNode[] labels = new LabelNode[mappings.size()];
		for (int i = 0; i < labels.length; i++) {
			String mapping = mappings.get(i);
			if (!mapping.matches("\\d+=.+"))
				throw new LineParseException(text, "Mapping, requires [value=label...]");
			String[] split = mapping.split("=");
			int key = Integer.parseInt(split[0]);
			LabelNode lbl = asm.getLabels().get(split[1]);
			if (lbl == null)
				throw new LineParseException(text, "No label by the given name: " + mapping);
			labels[i] = lbl;
			keys[i] = key;
		}
		// Decode default
		String dflt = ((List<String>) args.get(2)).get(0);
		LabelNode dfltLbl = asm.getLabels().get(dflt);
		if (dfltLbl == null)
			throw new LineParseException(text, "No label by the given name: " + dflt);
		//
		asm.appendInsn(new LookupSwitchInsnNode(dfltLbl, keys, labels));
	}
}
