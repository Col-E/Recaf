package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.insn.NamedLabelNode;
import me.coley.recaf.parse.assembly.Assembler;
import me.coley.recaf.parse.assembly.util.UniMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Label assembler
 * <pre>
 *     &lt;LABEL_TITLE&gt;
 * </pre>
 */
public class Label extends Assembler {
	/**
	 * Matcher for the label name.
	 */
	private final static UniMatcher<String> matcher
			= new UniMatcher<>("^[\\w-]+$", (s -> s));

	public Label(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		if(matcher.run(text)) {
			return new NamedLabelNode(matcher.get());
		}
		return fail(text, "Expected: <LABEL_TITLE>");
	}
}