package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.InsnUtil;
import me.coley.recaf.bytecode.OpcodeUtil;
import me.coley.recaf.bytecode.insn.NamedLookupSwitchInsnNode;
import me.coley.recaf.parse.assembly.AbstractAssembler;
import me.coley.recaf.parse.assembly.util.GroupMatcher;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.function.Function;

/**
 * LookupSwitch assembler
 * <pre>
 *     mapping[&lt;MAPPING&gt;...] default[&lt;OFFSET/LABEL&gt;]
 * Examples:
 *     mapping[0=A, 1=B, 2=C] default[D]
 *     map[0=A, 1=B, 2=C] dflt[D]
 *     [0=A, 1=B, 2=C] [D]
 * </pre>
 *
 * @author Matt
 */
public class LookupSwitch extends AbstractAssembler<LookupSwitchInsnNode> {
	/**
	 * Matcher for the switch.
	 */
	private final static GroupMatcher matcher =
			new GroupMatcher("^(mapping|map)?\\[({MAPPING}(\\d+=\\w+[,\\s]*)+)+\\]\\s(default|dflt)" +
					"?\\[({DEFAULT}.+)\\]",
					new HashMap<String, Function<String, Object>>() {{
						put("MAPPING", (s -> s));
						put("DEFAULT", (s -> s));
					}});

	public LookupSwitch(int opcode) {super(opcode);}

	@Override
	public LookupSwitchInsnNode parse(String text) {
		if(matcher.run(text)) {
			String mapping = matcher.get("MAPPING");
			String dflt = matcher.get("DEFAULT");
			String[] mappingSplit = mapping.split("[,\\s]+");
			if(mappingSplit.length == 0)
				fail(text, "Failed to parse mappings");
			String[] labels = new String[mappingSplit.length];
			int[] keys = new int[mappingSplit.length];
			int i = 0;
			for(String map : mappingSplit) {
				String[] mapSplit = map.split("=");
				keys[i] = Integer.parseInt(mapSplit[0]);
				labels[i] = mapSplit[1];
				i++;
			}
			return new NamedLookupSwitchInsnNode(dflt, labels, keys);
		}
		return fail(text, "Expected: mapping[<MAPPING>...] default[<OFFSET/LABEL>]");
	}

	@Override
	public String generate(MethodNode method, LookupSwitchInsnNode insn) {
		List<String> keys = new ArrayList<>();
		for (int i = 0; i < insn.keys.size(); i++)
			keys.add(insn.keys.get(i) + "=" + InsnUtil.labelName(insn.labels.get(i)));
		String dflt = InsnUtil.labelName(insn.dflt);
		return OpcodeUtil.opcodeToName(opcode) + " mapping[" + String.join(",", keys) + "] default[" + dflt + "]";
	}
}