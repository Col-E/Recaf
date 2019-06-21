package me.coley.recaf.parse.assembly.impl;

import com.github.javaparser.utils.StringEscapeUtils;
import me.coley.recaf.bytecode.OpcodeUtil;
import me.coley.recaf.parse.assembly.AbstractAssembler;
import me.coley.recaf.parse.assembly.util.UniMatcher;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * LDC assembler
 * <pre>
 *     &lt;VALUE&gt;
 * </pre>
 *
 * @author Matt
 */
public class Ldc extends AbstractAssembler<LdcInsnNode> {
	/**
	 * Matchers for the different types of values allowed in LDC instructions.
	 */
	private final static UniMatcher[] matchers = new UniMatcher[]{
			new UniMatcher<>("^-?\\d+\\.\\d*[fF]$|^-?\\d+[fF]$", (s -> Float.parseFloat(s))),
			new UniMatcher<>("^[-\\d]+$", (s ->  Integer.parseInt(s))),
			new UniMatcher<>("^-?\\d+\\.\\d*[dD]*$|^-?\\d+[dD]$", (s -> Double.parseDouble(s))),
			new UniMatcher<>("^[-\\d]+(?=[lLjJ]$)", (s ->  Long.parseLong(s))),
			new UniMatcher<>("^L.+;$", (s -> Type.getType(s))),
			new UniMatcher<>("(?!^\").*(?=\"$)", (s -> StringEscapeUtils.unescapeJava(s)))};

	public Ldc(int opcode) {super(opcode);}

	@Override
	public LdcInsnNode parse(String text) {
		for (UniMatcher matcher : matchers)
			if (matcher.run(text))
				return new LdcInsnNode(matcher.get());
		return fail(text, "Expected: <VALUE>");
	}

	@Override
	public String generate(MethodNode method, LdcInsnNode insn) {
		Object value = insn.cst;
		String s = value.toString();
		if(value instanceof String) {
			s = "\"" + StringEscapeUtils.escapeJava(s) + "\"";
		} else if(value instanceof Float) {
			s += "f";
		} else if(value instanceof Double) {
			s += "d";
		} else if(value instanceof Long) {
			s += "l";
		}
		return OpcodeUtil.opcodeToName(opcode) + " " + s;
	}
}