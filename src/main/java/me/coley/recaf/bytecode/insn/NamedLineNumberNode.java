package me.coley.recaf.bytecode.insn;

import java.util.Map;

import me.coley.recaf.parse.assembly.exception.LabelLinkageException;
import org.objectweb.asm.tree.*;

/**
 * Extension of LineNumberNode with an arbitrary opcode <i>(used internally to recaf)</i> used in serialization.
 * 
 * @author Matt
 */
public class NamedLineNumberNode extends LineNumberNode implements NamedLabelRefInsn {
	public static final int LINE_EXT = 301;
	/**
	 * Placeholder identifier for a label. The label is typically known after
	 * instantiation, thus making it impossible to provide in the constructor.
	 */
	private final String labelId;
	
	public NamedLineNumberNode(LineNumberNode line) {
		this(line.line, line.start, null);
	}

	public NamedLineNumberNode(int line, LabelNode start, String labelId) {
		super(line, start);
		this.labelId = labelId;
		this.opcode = LINE_EXT;
	}

	@Override
	public AbstractInsnNode clone(final Map<LabelNode, LabelNode> labels) {
		return new NamedLineNumberNode(line, labels.get(start), labelId);
	}

	@Override
	public AbstractInsnNode cleanClone(final Map<LabelNode, LabelNode> labels) {
		return new LineNumberNode(opcode, labels.get(start));
	}

	@Override
	public void setupLabels(Map<String, LabelNode> labels) {
		LabelNode lbl = labels.get(labelId);
		if (lbl == null)
			throw new LabelLinkageException(this, "Label identifier has no mapped value: " + labelId);
		start = lbl;
	}
}