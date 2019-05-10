package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.insn.NamedLookupSwitchInsnNode;
import me.coley.recaf.parse.assembly.Assembler;
import me.coley.recaf.parse.assembly.util.GroupMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.HashMap;
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
public class LookupSwitch extends Assembler {
	/**
	 * Matcher for the switch.
	 */
	private final static GroupMatcher matcher =
			new GroupMatcher("^(mapping|map)?\\[({MAPPING}(\\d=\\w[,\\s]*)+)+\\]\\s(default|dflt)" +
					"?\\[({DEFAULT}.+)\\]",
					new HashMap<String, Function<String, Object>>() {{
						put("MAPPING", (s -> s));
						put("DEFAULT", (s -> s));
					}});

	public LookupSwitch(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
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
}