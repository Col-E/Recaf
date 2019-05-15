package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.parse.assembly.TokenAssembler;
import me.coley.recaf.parse.assembly.util.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;

/**
 * Field assembler
 * <pre>
 *     &lt;HOST&gt;.&lt;NAME&gt; &lt;DESC&gt;
 * </pre>
 *
 * @author Matt
 */
public class Field extends TokenAssembler {
	public Field(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		RegexToken matcher = token();
		MatchResult result = matcher.matches(text);
		if(result.isSuccess()) {
			String owner = matcher.get("OWNER");
			String name = matcher.get("NAME");
			String desc = matcher.get("DESC");
			return new FieldInsnNode(opcode, owner, name, desc);
		}
		return fail(text, "Expected: <HOST>.<NAME> <DESC>", result.getFailedToken().getToken());
	}

	@Override
	public RegexToken createToken() {
		return RegexToken
				.create("OWNER", new UniMatcher<>("[$\\w\\/]+(?=\\.)", (s -> s)),
						((tok, part) -> AutoComplete.internalName(part)))
				.append("NAME", new UniMatcher<>("(?!=\\.)([$\\w]+)(?= )", (s->s)),
						((tok, part) -> AutoComplete.field(tok, part)))
				.append("DESC", new UniMatcher<>("(I$|J$|F$|D$|B$|C$|S$|V$|L[\\/$\\w]+;$)", (s->s)),
						((tok, part) -> AutoComplete.descriptorName(part)))
				.root();
	}
}