package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.parse.assembly.Assembler;
import me.coley.recaf.parse.assembly.util.GroupMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;

import java.util.HashMap;
import java.util.function.Function;

/**
 * Field assembler
 * <pre>
 *     &lt;HOST&gt;.&lt;NAME&gt; &lt;DESC&gt;
 * </pre>
 *
 * @author Matt
 */
public class Field extends Assembler {
	/**
	 * Matcher for the field.
	 */
	private final static GroupMatcher matcher =
			new GroupMatcher("({OWNER}[$\\w\\/]+(?=\\.))\\.({NAME}[$\\w]+) ({DESC}[\\/$\\w]+)",
					new HashMap<String, Function<String, Object>>() {{
						put("OWNER", (s -> s));
						put("NAME", (s -> s));
						put("DESC", (s -> s));
					}});

	public Field(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		if(matcher.run(text)) {
			String owner = matcher.get("OWNER");
			String name = matcher.get("NAME");
			String desc = matcher.get("DESC");
			return new FieldInsnNode(opcode, owner, name, desc);
		}
		return fail(text);
	}
}