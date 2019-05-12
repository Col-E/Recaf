package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.insn.NamedVarInsnNode;
import me.coley.recaf.parse.assembly.AbstractAssembler;
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
public class Var extends AbstractAssembler {
	/**
	 * Matcher for the variable posiiton.
	 */
	private final static UniMatcher<String> matcher =
			new UniMatcher<>("^\\w+$", (s -> s));

	public Var(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		if (matcher.run(text))
			return new NamedVarInsnNode(opcode, matcher.get());
		return fail(text, "Expected: <INDEX>");
	}
}