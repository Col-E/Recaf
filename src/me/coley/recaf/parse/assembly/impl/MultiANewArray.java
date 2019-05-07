package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.parse.assembly.Assembler;
import me.coley.recaf.parse.assembly.util.GroupMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;

import java.util.HashMap;
import java.util.function.Function;

/**
 * MultiANewArray assembler
 * <pre>
 *     &lt;TYPE&gt;&lt;LEVEL&gt;
 * </pre>
 *
 * @author Matt
 */
public class MultiANewArray extends Assembler {
	/**
	 * Matcher for the new array.
	 */
	private final static GroupMatcher matcher =
			new GroupMatcher("({TYPE}[\\w\\/]+)\\s+({DIMENSION}[\\d]+)",
					new HashMap<String, Function<String, Object>>() {{
						put("TYPE", (s -> s));
						put("DIMENSION", (s -> Integer.parseInt(s)));
					}});

	public MultiANewArray(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		if(matcher.run(text)) {
			String type = matcher.get("TYPE");
			int dimensions = matcher.get("DIMENSION");
			return new MultiANewArrayInsnNode(type, dimensions);
		}
		return fail(text);
	}
}