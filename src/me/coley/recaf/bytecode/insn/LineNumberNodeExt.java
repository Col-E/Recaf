package me.coley.recaf.bytecode.insn;

import java.util.Map;

import me.coley.recaf.parse.assembly.LabelLinkageException;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;

/**
 * Extension of LineNumberNode with an arbitrary opcode <i>(used internally to recaf)</i> used in serialization.
 * 
 * @author Matt
 */
public class LineNumberNodeExt extends LineNumberNode {
	public static final int LINE_EXT = 301;
	/**
	 * Placeholder identifier for a label. The label is typically known after
	 * instantiation, thus making it impossible to provide in the constructor.
	 */
	private final String labelId;
	
	public LineNumberNodeExt(LineNumberNode line) {
		this(line.line, line.start, null);
	}

	public LineNumberNodeExt(int line, LabelNode start, String labelId) {
		super(line, start);
		this.labelId = labelId;
		this.opcode = LINE_EXT;
	}

	@Override
	public AbstractInsnNode clone(final Map<LabelNode, LabelNode> labels) {
		return new LineNumberNodeExt(line, labels.get(start), labelId);
	}
	
	/**
	 * Set the label with a map of label identifiers to their instances.
	 * 
	 * @param labels
	 *            &lt;Identifier : Instance&gt;
	 */
	public void setupLabel(Map<String, LabelNode> labels) {
		LabelNode lbl = labels.get(labelId);
		if (lbl == null)
			throw new LabelLinkageException(this, "Label identifier has no mapped value: " + labelId);
		start = lbl;
	}
}