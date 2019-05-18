package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.InsnUtil;
import me.coley.recaf.bytecode.OpcodeUtil;
import me.coley.recaf.bytecode.insn.NamedTableSwitchInsnNode;
import me.coley.recaf.parse.assembly.AbstractAssembler;
import me.coley.recaf.parse.assembly.util.GroupMatcher;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.function.Function;

/**
 * TableSwitch assembler
 * <pre>
 *     range[&lt;RANGE&gt;] offsets[&lt;OFFSET/LABEL&gt;...] default[&lt;OFFSET/LABEL&gt;]
 * Examples:
 *     range[0-2] offsets[A, B, C] default[D]
 *     range[0-2] off[A, B, C] dflt[D]
 *     [0-2] [A, B, C] [D]
 * </pre>
 * Section identifiers may be shortened as shown, or not present,
 * as long as the content of each section is valid.
 *
 * @author Matt
 */
public class TableSwitch extends AbstractAssembler<TableSwitchInsnNode> {
	/**
	 * Matcher for the switch.
	 */
	private final static GroupMatcher matcher =
			new GroupMatcher("(range)?\\[({RANGE}\\d+-\\d+)\\](\\soffsets|\\soff|\\s)\\[" +
					"({OFFSETS}.+?)\\](\\sdflt|\\sdefault|\\s)\\[({DEFAULT}.+?)\\]",
					new HashMap<String, Function<String, Object>>() {{
						put("RANGE", (s -> s));
						put("OFFSETS", (s -> s));
						put("DEFAULT", (s -> s));
					}});

	public TableSwitch(int opcode) {super(opcode);}

	@Override
	public TableSwitchInsnNode parse(String text) {
		if(matcher.run(text)) {
			String range = matcher.get("RANGE");
			String offsets = matcher.get("OFFSETS");
			String dflt = matcher.get("DEFAULT");
			String[] rangeSplit = range.split("-");
			int min = Integer.parseInt(rangeSplit[0]);
			int max = Integer.parseInt(rangeSplit[1]);
			String[] offsetsSplit = offsets.split(",\\s?");
			if (offsetsSplit.length == 0)
				fail(text, "Failed to parse offsets");
			if ((max - min) != offsetsSplit.length - 1)
				fail(text, "Range difference size does not match number of given offsets");
			return new NamedTableSwitchInsnNode(min, max, dflt, offsetsSplit);
		}
		return fail(text, "Expected: range[<RANGE>] offsets[<OFFSET/LABEL>...] default[<OFFSET/LABEL>]");
	}

	@Override
	public String generate(MethodNode method, TableSwitchInsnNode insn) {
		List<String> offsets = new ArrayList<>();
		for (int i = 0; i < insn.labels.size(); i++)
			offsets.add(InsnUtil.labelName(insn.labels.get(i)));
		String range = insn.min + "-" + insn.max;
		String dflt = InsnUtil.labelName(insn.dflt);
		return OpcodeUtil.opcodeToName(opcode) + " range[" + range +  "] offsets[" + String.join(",", offsets) + "] default[" + dflt + "]";

	}
}