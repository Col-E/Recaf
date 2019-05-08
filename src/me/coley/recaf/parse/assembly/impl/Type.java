package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.parse.assembly.Assembler;
import me.coley.recaf.parse.assembly.util.AbstractMatcher;
import me.coley.recaf.parse.assembly.util.UniMatcher;
import org.objectweb.asm.tree.*;

/**
 * Type assembler
 * <pre>
 *     &lt;TYPE&gt;
 * </pre>
 *
 * @author Matt
 */
public class Type extends Assembler {
	/**
	 * Matcher for the type value.
	 */
	private final static UniMatcher<String> matcher =
			new UniMatcher<>("^[$\\w\\/]+$", (s -> s));

	public Type(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		if(matcher.run(text))
			return new TypeInsnNode(opcode, matcher.get());
		return fail(text, "Expected: <TYPE>");
	}
}