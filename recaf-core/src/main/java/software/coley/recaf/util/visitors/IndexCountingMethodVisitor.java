package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import software.coley.recaf.RecafConstants;

/**
 * Method visitor which counts the instruction index is currently being visited.
 * <p/>
 * When extending this type, you will want to access the current index before calling the super visit call.
 * For example:
 * <pre>
 * {@code
 * @Override
 * public void visitTypeInsn(int opcode, String type) {
 *    // Valid place to call getIndex() in a visit is at the start.
 *    int current = getIndex();
 *
 *    // ----> Your code goes here <----
 *
 *    // You MUST keep super-calls in your implementation to keep the index position accurate.
 *    super.visitTypeInsn(opcode, type);
 * }
 * }
 * </pre>
 *
 * @author Matt Coley
 */
public class IndexCountingMethodVisitor extends MethodVisitor {
	protected int index;

	/**
	 * @param mv
	 * 		Parent visitor.
	 */
	public IndexCountingMethodVisitor(@Nullable MethodVisitor mv) {
		super(RecafConstants.getAsmVersion(), mv);
	}

	/**
	 * @return Current index.
	 */
	public int getIndex() {
		return index;
	}

	@Override
	public void visitCode() {
		super.visitCode();
		index = 0;
	}

	@Override
	public void visitInsn(int opcode) {
		super.visitInsn(opcode);
		index++;
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		super.visitIntInsn(opcode, operand);
		index++;
	}

	@Override
	public void visitVarInsn(int opcode, int varIndex) {
		super.visitVarInsn(opcode, varIndex);
		index++;
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		super.visitTypeInsn(opcode, type);
		index++;
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		super.visitFieldInsn(opcode, owner, name, descriptor);
		index++;
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		index++;
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
		super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
		index++;
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		super.visitJumpInsn(opcode, label);
		index++;
	}

	@Override
	public void visitLabel(Label label) {
		super.visitLabel(label);
		index++;
	}

	@Override
	public void visitLdcInsn(Object value) {
		super.visitLdcInsn(value);
		index++;
	}

	@Override
	public void visitIincInsn(int varIndex, int increment) {
		super.visitIincInsn(varIndex, increment);
		index++;
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		super.visitTableSwitchInsn(min, max, dflt, labels);
		index++;
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		super.visitLookupSwitchInsn(dflt, keys, labels);
		index++;
	}

	@Override
	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		super.visitMultiANewArrayInsn(descriptor, numDimensions);
		index++;
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		super.visitLineNumber(line, start);
		index++;
	}
}
