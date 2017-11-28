package me.coley.recaf.asm.opcode;

import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;

/**
 * Extension of LineNumberNode with an arbitrary opcode used in serialization.
 * 
 * @author Matt
 */
public class LineNumberNodeExt extends LineNumberNode {
	public static final int LINE_EXT = 301;

	public LineNumberNodeExt(LineNumberNode line) {
		this(line.line, line.start);
	}

	public LineNumberNodeExt(int line, LabelNode start) {
		super(line, start);
		this.opcode = LINE_EXT;
	}

	@Override
	public AbstractInsnNode clone(final Map<LabelNode, LabelNode> labels) {
		return new LineNumberNodeExt(line, labels.get(start));
	}
}
