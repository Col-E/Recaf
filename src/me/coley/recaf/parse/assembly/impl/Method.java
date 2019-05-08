package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.parse.assembly.Assembler;
import me.coley.recaf.parse.assembly.util.GroupMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.HashMap;
import java.util.function.Function;

/**
 * Method essembler
 * <pre>
 *     &lt;HOST&gt;.&lt;NAME&gt;&lt;DESC&gt;
 * </pre>
 *
 * @author Matt
 */
public class Method extends Assembler {
	/**
	 * Matcher for the method.
	 */
	private final static GroupMatcher matcher =
			new GroupMatcher("({OWNER}[$\\w\\/]+(?=\\.))\\.({NAME}[$\\w]+(?=\\())({DESC}[();\\/$\\w]+)",
					new HashMap<String, Function<String, Object>>() {{
						put("OWNER", (s -> s));
						put("NAME", (s -> s));
						put("DESC", (s -> s));
					}});

	public Method(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		if(matcher.run(text)) {
			String owner = matcher.get("OWNER");
			String name = matcher.get("NAME");
			String desc = matcher.get("DESC");
			return new MethodInsnNode(opcode, owner, name, desc);
		}
		return fail(text, "Expected: <HOST>.<NAME><DESC>");
	}
}