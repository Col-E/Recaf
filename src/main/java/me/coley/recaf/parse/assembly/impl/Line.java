package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.InsnUtil;
import me.coley.recaf.bytecode.insn.LazyLineNumberNode;
import me.coley.recaf.bytecode.insn.NamedLineNumberNode;
import me.coley.recaf.parse.assembly.TokenAssembler;
import me.coley.recaf.parse.assembly.util.*;
import org.objectweb.asm.tree.*;


/**
 * Line number assembler
 * <pre>
 *     &lt;LINE_NO&gt; &lt;LABEL_TITLE&gt;
 * </pre>
 */
public class Line extends TokenAssembler<LineNumberNode> {
	public Line(int opcode) {super(opcode);}

	@Override
	public LineNumberNode parse(String text) {
		RegexToken matcher = token();
		MatchResult result = matcher.matches(text);
		if(result.isSuccess()) {
			int lineno = matcher.getMatch("LINENO");
			String lblName = matcher.get("LABEL");
			return new NamedLineNumberNode(lineno, null, lblName);
		} else if (matcher.has("LINENO")) {
			int lineno = matcher.getMatch("LINENO");
			return new LazyLineNumberNode(lineno);
		}
		return fail(text, "Expected: <LINE_NO> <LABEL_TITLE>");
	}

	@Override
	public String generate(MethodNode method, LineNumberNode insn) {
		String label = InsnUtil.labelName(insn.start);
		return "LINE " + insn.line + " " + label;
	}

	@Override
	public RegexToken createToken() {
		return RegexToken
				.create("LINENO", new UniMatcher<>("\\d+", (s -> Integer.parseInt(s))),
						((tok, part) -> AutoComplete.internalName(part)))
				.append("LABEL", new UniMatcher<>("(?!= )([\\w-]+)", (s->s)),
						((tok, part) -> AutoComplete.field(tok, part)))
				.root();
	}
}