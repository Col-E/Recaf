package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.insn.NamedJumpInsnNode;
import me.coley.recaf.parse.assembly.Assembler;
import me.coley.recaf.parse.assembly.util.UniMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Jump essembler
 * <pre>
 *     &lt;LABEL_TITLE&gt;
 * </pre>
 *
 * @author Matt
 */
public class Jump extends Assembler {
	/**
	 * Matcher for the label name.
	 */
	private final static UniMatcher<String> matcher
			= new UniMatcher<>("^[\\w-]+$", (s -> s));

	public Jump(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		if(matcher.run(text)) {
			return new NamedJumpInsnNode(opcode, matcher.get());
		}
		return fail(text, "Expected: <LABEL_TITLE>");
	}
}