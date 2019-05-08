package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.parse.assembly.Assembler;
import me.coley.recaf.parse.assembly.util.AbstractMatcher;
import me.coley.recaf.parse.assembly.util.UniMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Local variable assembler
 * <pre>
 *     &lt;INDEX&gt;
 * </pre>
 *
 * @author Matt
 */
public class Var extends Assembler {
	/**
	 * Matcher for the variable posiiton.
	 */
	private final static UniMatcher<Integer> matcher =
			new UniMatcher<>("^\\d+$", (s -> Integer.parseInt(s)));

	public Var(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		if (matcher.run(text))
			return new VarInsnNode(opcode, matcher.get());
		return fail(text, "Expected: <INDEX>");
	}
}