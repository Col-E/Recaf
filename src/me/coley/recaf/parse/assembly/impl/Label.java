package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.InsnUtil;
import me.coley.recaf.bytecode.insn.NamedLabelNode;
import me.coley.recaf.parse.assembly.AbstractAssembler;
import me.coley.recaf.parse.assembly.util.UniMatcher;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Label assembler
 * <pre>
 *     &lt;LABEL_TITLE&gt;
 * </pre>
 */
public class Label extends AbstractAssembler<LabelNode> {
	/**
	 * Matcher for the label name.
	 */
	private final static UniMatcher<String> matcher = new UniMatcher<>("^[\\w-]+$", (s -> s));

	public Label(int opcode) {super(opcode);}

	@Override
	public LabelNode parse(String text) {
		if(matcher.run(text)) {
			return new NamedLabelNode(matcher.get());
		}
		return fail(text, "Expected: <LABEL_TITLE>");
	}

	@Override
	public String generate(MethodNode method, LabelNode insn) {
		return "LABEL " + InsnUtil.labelName(insn);
	}
}