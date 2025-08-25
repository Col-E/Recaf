package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;
import software.coley.recaf.RecafConstants;

/**
 * Method visitor that skips over all content by default.
 *
 * @author Matt Coley
 */
public class SkippingMethodVisitor extends MethodVisitor {
	public SkippingMethodVisitor() {
		this(null);
	}

	public SkippingMethodVisitor(@Nullable MethodVisitor mv) {
		super(RecafConstants.getAsmVersion(), mv);
	}

	@Override
	public void visitParameter(String name, int access) {
		// no-op
	}

	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		return null;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return null;
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		return null;
	}

	@Override
	public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
		// no-op
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
		return null;
	}

	@Override
	public void visitAttribute(Attribute attribute) {
		// no-op
	}

	@Override
	public void visitCode() {
		// no-op
	}

	@Override
	public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
		// no-op
	}

	@Override
	public void visitInsn(int opcode) {
		// no-op
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		// no-op
	}

	@Override
	public void visitVarInsn(int opcode, int varIndex) {
		// no-op
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		// no-op
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		// no-op
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
		// no-op
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		// no-op
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
	                                   Object... bootstrapMethodArguments) {
		// no-op
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		// no-op
	}

	@Override
	public void visitLabel(Label label) {
		// no-op
	}

	@Override
	public void visitLdcInsn(Object value) {
		// no-op
	}

	@Override
	public void visitIincInsn(int varIndex, int increment) {
		// no-op
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		// no-op
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		// no-op
	}

	@Override
	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		// no-op
	}

	@Override
	public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		return null;
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		// no-op
	}

	@Override
	public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		return null;
	}

	@Override
	public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
		// no-op
	}

	@Override
	public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
		return null;
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		// no-op
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		// no-op
	}

	@Override
	public void visitEnd() {
		// no-op
	}
}
