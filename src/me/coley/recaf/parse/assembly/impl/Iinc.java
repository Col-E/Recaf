package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.parse.assembly.Assembler;
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
public class Iinc extends Assembler {
	/**
	 * Matcher for the increment values.
	 */
	private final static GroupMatcher matcher =
			new GroupMatcher("({INDEX}\\d+)\\s+({OPERAND}[-+])?\\s*({INCREMENT}\\d+)",
					new HashMap<String, Function<String, Object>>() {{
						put("INDEX", (s -> Integer.parseInt(s)));
						put("OPERAND", (s -> s));
						put("INCREMENT", (s -> Integer.parseInt(s)));
					}});

	public Iinc(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		if(matcher.run(text)) {
			int index = matcher.get("INDEX");
			int increment = matcher.get("INCREMENT");
			// Optional operand to specify negative numbers
			String op = matcher.get("OPERAND");
			if(op != null && op.equalsIgnoreCase("-")) {
				increment = -increment;
			}
			return new IincInsnNode(index, increment);
		}
		return null;
	}
}