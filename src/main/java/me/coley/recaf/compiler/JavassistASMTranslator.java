package me.coley.recaf.compiler;

import javassist.CtBehavior;
import me.coley.recaf.util.OpcodeUtil;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of {@link JavassistMethodTranslator} for ASM.
 *
 * @author Matt
 */
public class JavassistASMTranslator extends JavassistMethodTranslator {
	private final InsnList list = new InsnListNullFix();
	private final Map<Integer, LabelNode> offsetToLabel = new HashMap<>();

	@Override
	public void visitBranchDestination(int offset) {
		LabelNode l = getLabel(offset);
		list.add(l);
	}

	@Override
	public void visitJump(int opcode, int offset) {
		LabelNode l = getLabel(offset);
		list.add(new JumpInsnNode(opcode, l));
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
	 * @param method
	 * 		Base Javassist method to copy method definition info from.
	 *
	 * @return ASM node representation of translated method.
	 */
	public MethodNode toAsmMethod(CtBehavior method) {
		MethodNode m = new MethodNode(method.getModifiers(), method.getName(), method.getSignature(), null, null);
		m.instructions = list;
		return m;
	}

	/**
	 * An extension of {@link InsnList} that fixes an odd bug with the Javassist generated code.
	 * For some reason, some {@code null} values are added at the end of the list's array cache.
	 * This cuts off those invalid entries.
	 *
	 * @author Matt
	 */
	private static class InsnListNullFix extends InsnList {
		@Override
		public AbstractInsnNode[] toArray() {
			AbstractInsnNode[] array = super.toArray();
			int newLength = array.length;
			while (array[newLength - 1] == null)
				newLength--;
			return Arrays.copyOf(array, newLength);
		}
	}
}
