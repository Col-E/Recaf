package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.OpcodeUtil;
import me.coley.recaf.parse.assembly.TokenAssembler;
import me.coley.recaf.parse.assembly.util.*;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Field assembler
 * <pre>
 *     &lt;HOST&gt;.&lt;NAME&gt; &lt;DESC&gt;
 * </pre>
 *
 * @author Matt
 */
public class Field extends TokenAssembler<FieldInsnNode> {
	public Field(int opcode) {super(opcode);}

	@Override
	public FieldInsnNode parse(String text) {
		RegexToken matcher = token();
		MatchResult result = matcher.matches(text);
		if(result.isSuccess()) {
			String owner = matcher.getMatch("OWNER");
			String name = matcher.getMatch("NAME");
			String desc = matcher.getMatch("DESC");
			return new FieldInsnNode(opcode, owner, name, desc);
		}
		return fail(text, "Expected: <HOST>.<NAME> <DESC>", result.getFailedToken().getToken());
	}

	@Override
	public String generate(MethodNode method, FieldInsnNode insn) {
		String host = insn.owner;
		String name = insn.name;
		String desc = insn.desc;
		return OpcodeUtil.opcodeToName(opcode) + " " + host + "." + name + " " + desc;
	}

	@Override
	public RegexToken createToken() {
		return RegexToken
				.create("OWNER", new UniMatcher<>("[$\\w\\/]+(?=\\.)", (s -> s)),
						((tok, part) -> AutoComplete.internalName(part)))
				.append("NAME", new UniMatcher<>("(?!=\\.)([$\\w]+)(?= )", (s->s)),
						((tok, part) -> AutoComplete.field(tok, part)))
				.append("DESC", new UniMatcher<>("(\\[*I$|\\[*J$|\\[*F$|\\[*D$|\\[*B$|\\[*C$|\\[*S$|\\[*Z$|V$|\\[*L[\\/$\\w]+;$)", (s->s)),
						((tok, part) -> AutoComplete.descriptorName(part)))
				.root();
	}
}