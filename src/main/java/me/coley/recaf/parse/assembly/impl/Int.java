package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.OpcodeUtil;
import me.coley.recaf.parse.assembly.AbstractAssembler;
import me.coley.recaf.parse.assembly.util.UniMatcher;
import org.objectweb.asm.tree.*;

/**
 * Integer essembler
 * <pre>
 *     &lt;VALUE&gt;
 * </pre>
 *
 * @author Matt
 */
public class Int extends AbstractAssembler<IntInsnNode> {
	/**
	 * Matcher for the variable posiiton.
	 */
	private final static UniMatcher<Integer> matcher =
			new UniMatcher<>("\\d+", (s -> Integer.parseInt(s)));

	public Int(int opcode) {super(opcode);}

	@Override
	public IntInsnNode parse(String text) {
		if (matcher.run(text))
			return new IntInsnNode(opcode, matcher.get());
		return fail(text, "Expected: <VALUE>");
	}

	@Override
	public String generate(MethodNode method, IntInsnNode insn) {
		return OpcodeUtil.opcodeToName(opcode) + " " + insn.operand;
	}
}