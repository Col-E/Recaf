package me.coley.recaf.parse.assembly.impl;

import me.coley.recaf.bytecode.insn.NamedLineNumberNode;
import me.coley.recaf.parse.assembly.AbstractAssembler;
import me.coley.recaf.parse.assembly.util.GroupMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.HashMap;
import java.util.function.Function;

/**
 * Line number assembler
 * <pre>
 *     &lt;LINE_NO&gt; &lt;LABEL_TITLE&gt;
 * </pre>
 */
public class Line extends AbstractAssembler {
	/**
	 * Matcher for the increment values.
	 */
	private final static GroupMatcher matcher =
			new GroupMatcher("({LINENO}\\d+)\\s+({LABEL}[\\w-]+)?$)",
					new HashMap<String, Function<String, Object>>() {{
						put("LINENO", (s -> Integer.parseInt(s)));
						put("LABEL", (s -> s));
					}});

	public Line(int opcode) {super(opcode);}

	@Override
	public AbstractInsnNode parse(String text) {
		if(matcher.run(text)) {
			int lineno = matcher.get("LINENO");
			String lblName = matcher.get("LABEL");
			return new NamedLineNumberNode(lineno, null, lblName);
		}
		return fail(text, "Expected: <LINE_NO> <LABEL_TITLE>");
	}
}