package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.OpcodeUtil;
import me.coley.recaf.parse.assembly.TokenAssembler;
import me.coley.recaf.parse.assembly.util.*;
import org.objectweb.asm.tree.*;

import java.util.Collections;

/**
 * Method essembler
 * <pre>
 *     &lt;HOST&gt;.&lt;NAME&gt;&lt;DESC&gt;
 * </pre>
 *
 * @author Matt
 */
public class Method extends TokenAssembler<MethodInsnNode> {
	public Method(int opcode) {super(opcode);}

	@Override
	public MethodInsnNode parse(String text) {
		RegexToken matcher = token();
		MatchResult result = matcher.matches(text);
		if(result.isSuccess()) {
			String owner = matcher.getMatch("OWNER");
			String name = matcher.getMatch("NAME");
			String desc = matcher.getMatch("DESC");
			// TODO: Replace this quick hack after https://gitlab.ow2.org/asm/asm/issues/317875 is fixed
			if (desc.contains("(") && !desc.contains(")")) {
				fail(text, "DESC does not close parameters");
			}
			return new MethodInsnNode(opcode, owner, name, desc);
		}
		return fail(text, "Expected: <HOST>.<NAME><DESC>");
	}

	@Override
	public String generate(MethodNode method, MethodInsnNode insn) {
		String host = insn.owner;
		String name = insn.name;
		String desc = insn.desc;
		return OpcodeUtil.opcodeToName(opcode) + " "+ host + "." + name + desc;
	}

	@Override
	public RegexToken createToken() {
		return RegexToken
				.create("OWNER", new UniMatcher<>("[$\\w\\/]+(?=\\.)", (s -> s)),
						((tok, part) -> AutoComplete.internalName(part)))
				.append("NAME", new UniMatcher<>("(?!=\\.)([<>$\\w]+)(?=\\()", (s->s)),
						((tok, part) -> AutoComplete.method(tok, part)))
				.append("DESC", new UniMatcher<>("([\\[();\\/$\\w]+)", (s->s)),
						((tok, part) -> Collections.emptyList()))
				.root();
	}
}