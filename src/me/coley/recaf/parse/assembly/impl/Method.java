package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.parse.assembly.AbstractAssembler;
import me.coley.recaf.parse.assembly.TokenAssembler;
import me.coley.recaf.parse.assembly.util.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.function.Function;

/**
 * Method essembler
 * <pre>
 *     &lt;HOST&gt;.&lt;NAME&gt;&lt;DESC&gt;
 * </pre>
 *
 * @author Matt
 */
public class Method extends TokenAssembler {
	public Method(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		RegexToken matcher = token();
		MatchResult result = matcher.matches(text);
		if(result.isSuccess()) {
			String owner = matcher.get("OWNER");
			String name = matcher.get("NAME");
			String desc = matcher.get("DESC");
			return new MethodInsnNode(opcode, owner, name, desc);
		}
		return fail(text, "Expected: <HOST>.<NAME><DESC>");
	}

	@Override
	public RegexToken createToken() {
		return RegexToken
				.create("OWNER", new UniMatcher<>("[$\\w\\/]+(?=\\.)", (s -> s)),
						((tok, part) -> AutoComplete.internalName(part)))
				.append("NAME", new UniMatcher<>("(?!=\\.)([<>$\\w]+)(?=\\()", (s->s)),
						((tok, part) -> AutoComplete.method(tok, part)))
				.append("DESC", new UniMatcher<>("([();\\/$\\w]+)", (s->s)),
						((tok, part) -> Collections.emptyList()))
				.root();
	}
}