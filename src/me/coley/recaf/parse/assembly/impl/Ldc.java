package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.parse.assembly.*;
import me.coley.recaf.parse.assembly.util.UniMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

/**
 * LDC assembler
 * <pre>
 *     &lt;VALUE&gt;
 * </pre>
 *
 * @author Matt
 */
public class Ldc extends AbstractAssembler {
	/**
	 * Matchers for the different types of values allowed in LDC instructions.
	 */
	private final static UniMatcher[] matchers = new UniMatcher[]{
			new UniMatcher<>("^-?\\d+\\.\\d*[fF]$|^-?\\d+[fF]$", (s -> Float.parseFloat(s))),
			new UniMatcher<>("^[-\\d]+$", (s ->  Integer.parseInt(s))),
			new UniMatcher<>("^-?\\d+\\.\\d*[dD]*$|^-?\\d+[dD]$", (s -> Double.parseDouble(s))),
			new UniMatcher<>("^[-\\d]+(?=[lLjJ]$)", (s ->  Long.parseLong(s))),
			new UniMatcher<>("(?!^\").+(?=\"$)", (s -> s))};

	public Ldc(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		for (UniMatcher matcher : matchers)
			if (matcher.run(text))
				return new LdcInsnNode(matcher.get());
		return fail(text, "Expected: <VALUE>");
	}
}