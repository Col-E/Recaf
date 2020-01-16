package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.Variables;
import org.objectweb.asm.tree.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Table switch instruction AST.
 *
 * @author Matt
 */
public class LookupSwitchInsnAST extends InsnAST {
	private final Map<NumberAST, NameAST> mapping;
	private final NameAST dfltLabel;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param opcode
	 * 		Opcode AST.
	 * @param mapping
	 * 		Mapping of values to labels.
	 * @param dfltLabel
	 * 		Default fallback label AST.
	 */
	public LookupSwitchInsnAST(int line, int start, OpcodeAST opcode,
							   Map<NumberAST, NameAST> mapping, NameAST dfltLabel) {
		super(line, start, opcode);
		this.mapping = mapping;
		this.dfltLabel = dfltLabel;
		mapping.forEach((k, v) -> {
			addChild(k);
			addChild(v);
		});
		addChild(dfltLabel);
	}

	/**
	 * @return Mapping of values to labels.
	 */
	public Map<NumberAST, NameAST> getMapping() {
		return mapping;
	}

	/**
	 * @return Default fallback label AST.
	 */
	public NameAST getDfltLabel() {
		return dfltLabel;
	}

	@Override
	public String print() {
		String map = mapping.entrySet().stream()
				.map(e -> e.getKey().print() + "=" + e.getValue().print())
				.sorted()
				.collect(Collectors.joining(", "));
		return getOpcode().print() +
				" mapping[" + map + "]" +
				" default[" + dfltLabel.print() + "]";
	}

	@Override
	public AbstractInsnNode compile(Map<String, LabelNode> labels, Variables variables) {
		int[] keys = new int[mapping.size()];
		LabelNode[] lbls = new LabelNode[mapping.size()];
		int i = 0;
		for(Map.Entry<NumberAST, NameAST> entry : mapping.entrySet()) {
			int key = entry.getKey().getIntValue();
			LabelNode lbl = labels.get(entry.getValue().getName());
			keys[i] = key;
			lbls[i] = lbl;
			i++;
		}
		LabelNode dflt = labels.get(getDfltLabel().getName());
		return new LookupSwitchInsnNode(dflt, keys, lbls);
	}
}
