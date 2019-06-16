package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.*;
import me.coley.recaf.bytecode.insn.NamedVarInsnNode;
import me.coley.recaf.parse.assembly.AbstractAssembler;
import me.coley.recaf.parse.assembly.util.NamedVariableGenerator;
import me.coley.recaf.parse.assembly.util.UniMatcher;
import org.objectweb.asm.tree.*;

/**
 * Local variable assembler
 * <pre>
 *     &lt;INDEX&gt;
 * </pre>
 *
 * @author Matt
 */
public class Var extends AbstractAssembler<VarInsnNode> implements NamedVariableGenerator {
	/**
	 * Matcher for the variable posiiton.
	 */
	private final static UniMatcher<String> matcher =
			new UniMatcher<>("^\\w+$", (s -> s));

	public Var(int opcode) {super(opcode);}

	@Override
	public VarInsnNode parse(String text) {
		if (matcher.run(text))
			return new NamedVarInsnNode(opcode, matcher.get());
		return fail(text, "Expected: <VARIABLE>");
	}

	@Override
	public String generate(MethodNode method, VarInsnNode insn) {
		int index = insn.var;
		LocalVariableNode lvn = InsnUtil.getLocal(method, index);
		String variable = null;
		if(lvn == null) {
			// Use "this" when possible
			if(index == 0 && !AccessFlag.isStatic(method.access)) {
				variable = "this";
			} else {
				variable = String.valueOf(index);
			}
		} else {
			variable = name(method, index, lvn.name);
		}
		return OpcodeUtil.opcodeToName(opcode) + " " + variable;
	}
}