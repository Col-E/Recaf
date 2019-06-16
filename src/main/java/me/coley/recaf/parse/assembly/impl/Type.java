package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.OpcodeUtil;
import me.coley.recaf.parse.assembly.TokenAssembler;
import me.coley.recaf.parse.assembly.util.*;
import org.objectweb.asm.tree.*;

import java.util.List;

/**
 * Type assembler
 * <pre>
 *     &lt;TYPE&gt;
 * </pre>
 *
 * @author Matt
 */
public class Type extends TokenAssembler<TypeInsnNode> {
	public Type(int opcode) {super(opcode);}

	@Override
	public TypeInsnNode parse(String text) {
		RegexToken matcher = token();
		MatchResult result = matcher.matches(text);
		if(result.isSuccess()) {
			return new TypeInsnNode(opcode, matcher.getMatch("TYPE"));
		}
		return fail(text, "Expected: <TYPE>", result.getFailedToken().getToken());
	}

	@Override
	public String generate(MethodNode method, TypeInsnNode insn) {
		return OpcodeUtil.opcodeToName(opcode) + " " + insn.desc;
	}

	@Override
	public RegexToken createToken() {
		return RegexToken
				.create("TYPE", new UniMatcher<>("^[\\[;$\\w\\/]+$", (s -> s)),
						((tok, part) -> AutoComplete.internalName(part)));
	}

	@Override
	public List<String> suggest(String text) {
		// Invoke the tokenizer
		token().matches(text);
		// Fetch suggestions
		return token().suggest();
	}
}