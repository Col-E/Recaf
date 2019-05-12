package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.insn.NamedIincInsnNode;
import me.coley.recaf.parse.assembly.AbstractAssembler;
import me.coley.recaf.parse.assembly.util.GroupMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;

import java.util.HashMap;
import java.util.function.Function;

/**
 * Iinc assembler
 * <pre>
 *     &lt;VARIABLE&gt; &lt;OPERATION&gt; &lt;VALUE&gt;
 * </pre>
 *
 * @author Matt
 */
public class Iinc extends AbstractAssembler {
	/**
	 * Matcher for the increment values.
	 */
	private final static GroupMatcher matcher =
			new GroupMatcher("({INDEX}\\w+)\\s+({OPERAND}[-+])?\\s*({INCREMENT}\\d+)",
					new HashMap<String, Function<String, Object>>() {{
						put("INDEX", (s -> s));
						put("OPERAND", (s -> s));
						put("INCREMENT", (s -> Integer.parseInt(s)));
					}});

	public Iinc(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		if(matcher.run(text)) {
			String index = matcher.get("INDEX");
			int increment = matcher.get("INCREMENT");
			// Optional operand to specify negative numbers
			String op = matcher.get("OPERAND");
			if(op != null && op.equalsIgnoreCase("-")) {
				increment = -increment;
			}
			return new NamedIincInsnNode(increment, index);
		}
		return fail(text, "Expected: <VARIABLE> <OPERATION> <VALUE>");
	}
}