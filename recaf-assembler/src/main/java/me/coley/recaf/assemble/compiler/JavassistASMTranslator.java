package me.coley.recaf.assemble.compiler;

import me.coley.recaf.util.OpcodeUtil;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * An implementation of {@link JavassistMethodTranslator} for ASM.
 *
 * @author Matt Coley
 */
public class JavassistASMTranslator extends JavassistMethodTranslator {
	private final List<TryCatchBlockNode> tryBlocks = new ArrayList<>();
	private final InsnList list = new InsnList();
	private final Map<Integer, LabelNode> offsetToLabel = new HashMap<>();
	private final Set<Integer> visitedOffsets = new HashSet<>();

	@Override
	public void visitBranchDestination(int offset) {
		if (!visitedOffsets.contains(offset)) {
			LabelNode l = getLabel(offset);
			list.add(l);
			visitedOffsets.add(offset);
		}
	}

	@Override
	public void visitTryCatch(int start, int end, int handler, String typeName) {
		LabelNode s = getLabel(start);
		LabelNode e = getLabel(end);
		LabelNode h = getLabel(handler);
		tryBlocks.add(new TryCatchBlockNode(s, e, h, typeName));
	}

	@Override
	public void visitJump(int opcode, int offset) {
		LabelNode l = getLabel(offset);
		list.add(new JumpInsnNode(opcode, l));
	}

	@Override
	public void visitLookupSwitch(int opcode, int defaultPc, int[] keys, int[] pcs) {
		LabelNode d = getLabel(defaultPc);
		LabelNode[] lbls = new LabelNode[pcs.length];
		for (int i = 0; i < pcs.length; i++)
			lbls[i] = getLabel(pcs[i]);
		list.add(new LookupSwitchInsnNode(d, keys, lbls));
	}

	@Override
	public void visitTableSwitch(int opcode, int defaultPc, int min, int max, int[] pcs) {
		LabelNode d = getLabel(defaultPc);
		LabelNode[] lbls = new LabelNode[pcs.length];
		for (int i = 0; i < pcs.length; i++)
			lbls[i] = getLabel(pcs[i]);
		list.add(new TableSwitchInsnNode(min, max, d, lbls));
	}

	@Override
	public void visitMultiANewArray(int opcode, String type, int dimensions) {
		list.add(new MultiANewArrayInsnNode(type, dimensions));
	}

	@Override
	public void visitLdc(int opcode, Object value) {
		list.add(new LdcInsnNode(value));
	}

	@Override
	public void visitIinc(int opcode, int index, int incr) {
		list.add(new IincInsnNode(index, incr));
	}

	@Override
	public void visitVar(int opcode, int index) {
		list.add(new VarInsnNode(OpcodeUtil.deindexVarOp(opcode), index));
	}

	@Override
	public void visitMethod(int opcode, String owner, String name, String desc) {
		list.add(new MethodInsnNode(opcode, owner, name, desc));
	}

	@Override
	public void visitField(int opcode, String owner, String name, String desc) {
		list.add(new FieldInsnNode(opcode, owner, name, desc));
	}

	@Override
	public void visitInt(int opcode, int value) {
		list.add(new IntInsnNode(opcode, value));
	}

	@Override
	public void visitType(int opcode, String type) {
		list.add(new TypeInsnNode(opcode, type));
	}

	@Override
	public void visitInsn(int opcode) {
		list.add(new InsnNode(opcode));
	}

	private LabelNode getLabel(int offset) {
		return offsetToLabel.computeIfAbsent(offset, k -> new LabelNode());
	}

	/**
	 * @return Translated instructions.
	 */
	public InsnList getInstructions() {
		return list;
	}

	/**
	 * @return Translated try blocks.
	 */
	public List<TryCatchBlockNode> getTryBlocks() {
		return tryBlocks;
	}
}
