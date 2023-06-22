package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.*;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.member.ClassMember;

import java.util.HashMap;
import java.util.Map;

/**
 * Visitor that copies a member with a new name.
 *
 * @author Matt Coley
 */
public class MemberCopyingVisitor extends ClassVisitor {
	private final ClassMember member;
	private final String copyName;

	/**
	 * @param cv
	 * 		Parent visitor where the copy will be applied in.
	 * @param member
	 * 		Member to copy.
	 * @param copyName
	 * 		Name of copied member.
	 */
	public MemberCopyingVisitor(@Nullable ClassVisitor cv,
								@Nonnull ClassMember member,
								@Nonnull String copyName) {
		super(RecafConstants.getAsmVersion(), cv);
		this.member = member;
		this.copyName = copyName;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
		if (member.isField() && member.getName().equals(name) && member.getDescriptor().equals(desc)) {
			FieldVisitor original = super.visitField(access, name, desc, sig, value);
			FieldVisitor copy = super.visitField(access, copyName, desc, sig, value);
			return new CopyingFieldVisitor(original, copy);
		} else {
			return super.visitField(access, name, desc, sig, value);
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
		if (member.isMethod() && member.getName().equals(name) && member.getDescriptor().equals(desc)) {
			MethodVisitor original = super.visitMethod(access, name, desc, sig, exceptions);
			MethodVisitor copy = super.visitMethod(access, copyName, desc, sig, exceptions);
			return new CopyingMethodVisitor(original, copy);
		} else {
			return super.visitMethod(access, name, desc, sig, exceptions);
		}
	}

	private static class CopyingFieldVisitor extends FieldVisitor {
		private final FieldVisitor copy;

		public CopyingFieldVisitor(FieldVisitor original, FieldVisitor copy) {
			super(RecafConstants.getAsmVersion(), original);
			this.copy = copy;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			AnnotationVisitor annoOriginal = super.visitAnnotation(desc, visible);
			AnnotationVisitor annoCopy = copy.visitAnnotation(desc, visible);
			return new CopyingAnnotationVisitor(annoOriginal, annoCopy);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor annoOriginal = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
			AnnotationVisitor annoCopy = copy.visitTypeAnnotation(typeRef, typePath, desc, visible);
			return new CopyingAnnotationVisitor(annoOriginal, annoCopy);
		}

		@Override
		public void visitAttribute(Attribute attribute) {
			super.visitAttribute(attribute);
			copy.visitAttribute(attribute);
		}
	}

	private static class CopyingMethodVisitor extends MethodVisitor {
		private final Map<Label, Label> labelMap = new HashMap<>();
		private final MethodVisitor copy;

		public CopyingMethodVisitor(MethodVisitor original, MethodVisitor copy) {
			super(RecafConstants.getAsmVersion(), original);
			this.copy = copy;
		}

		@Override
		public void visitParameter(String name, int access) {
			super.visitParameter(name, access);
			copy.visitParameter(name, access);
		}

		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			AnnotationVisitor annoOriginal = super.visitAnnotationDefault();
			AnnotationVisitor annoCopy = copy.visitAnnotationDefault();
			return new CopyingAnnotationVisitor(annoOriginal, annoCopy);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			AnnotationVisitor annoOriginal = super.visitAnnotation(desc, visible);
			AnnotationVisitor annoCopy = copy.visitAnnotation(desc, visible);
			return new CopyingAnnotationVisitor(annoOriginal, annoCopy);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor annoOriginal = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
			AnnotationVisitor annoCopy = copy.visitTypeAnnotation(typeRef, typePath, desc, visible);
			return new CopyingAnnotationVisitor(annoOriginal, annoCopy);
		}

		@Override
		public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
			super.visitAnnotableParameterCount(parameterCount, visible);
			copy.visitAnnotableParameterCount(parameterCount, visible);
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
			AnnotationVisitor annoOriginal = super.visitParameterAnnotation(parameter, desc, visible);
			AnnotationVisitor annoCopy = copy.visitParameterAnnotation(parameter, desc, visible);
			return new CopyingAnnotationVisitor(annoOriginal, annoCopy);
		}

		@Override
		public void visitAttribute(Attribute attribute) {
			super.visitAttribute(attribute);
			copy.visitAttribute(attribute);
		}

		@Override
		public void visitCode() {
			super.visitCode();
			copy.visitCode();
		}

		@Override
		public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
			super.visitFrame(type, numLocal, local, numStack, stack);
			copy.visitFrame(type, numLocal, local, numStack, stack);
		}

		@Override
		public void visitInsn(int opcode) {
			super.visitInsn(opcode);
			copy.visitInsn(opcode);
		}

		@Override
		public void visitIntInsn(int opcode, int operand) {
			super.visitIntInsn(opcode, operand);
			copy.visitIntInsn(opcode, operand);
		}

		@Override
		public void visitVarInsn(int opcode, int var) {
			super.visitVarInsn(opcode, var);
			copy.visitVarInsn(opcode, var);
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			super.visitTypeInsn(opcode, type);
			copy.visitTypeInsn(opcode, type);
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			super.visitFieldInsn(opcode, owner, name, desc);
			copy.visitFieldInsn(opcode, owner, name, desc);
		}

		@Override
		@SuppressWarnings("deprecation")
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			super.visitMethodInsn(opcode, owner, name, desc);
			copy.visitMethodInsn(opcode, owner, name, desc);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
			super.visitMethodInsn(opcode, owner, name, desc, isInterface);
			copy.visitMethodInsn(opcode, owner, name, desc, isInterface);
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String desc, Handle bootstrapMethodHandle,
										   Object... bootstrapMethodArguments) {
			super.visitInvokeDynamicInsn(name, desc, bootstrapMethodHandle, bootstrapMethodArguments);
			copy.visitInvokeDynamicInsn(name, desc, bootstrapMethodHandle, bootstrapMethodArguments);
		}

		@Override
		public void visitJumpInsn(int opcode, Label label) {
			super.visitJumpInsn(opcode, label);
			copy.visitJumpInsn(opcode, clone(label));
		}

		@Override
		public void visitLabel(Label label) {
			super.visitLabel(label);
			copy.visitLabel(clone(label));
		}

		@Override
		public void visitLdcInsn(Object value) {
			super.visitLdcInsn(value);
			copy.visitLdcInsn(value);
		}

		@Override
		public void visitIincInsn(int var, int increment) {
			super.visitIincInsn(var, increment);
			copy.visitIincInsn(var, increment);
		}

		@Override
		public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
			super.visitTableSwitchInsn(min, max, dflt, labels);
			copy.visitTableSwitchInsn(min, max, clone(dflt), clone(labels));
		}

		@Override
		public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
			super.visitLookupSwitchInsn(dflt, keys, labels);
			copy.visitLookupSwitchInsn(clone(dflt), keys, clone(labels));
		}

		@Override
		public void visitMultiANewArrayInsn(String desc, int numDimensions) {
			super.visitMultiANewArrayInsn(desc, numDimensions);
			copy.visitMultiANewArrayInsn(desc, numDimensions);
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor annoOriginal = super.visitInsnAnnotation(typeRef, typePath, desc, visible);
			AnnotationVisitor annoCopy = copy.visitInsnAnnotation(typeRef, typePath, desc, visible);
			return new CopyingAnnotationVisitor(annoOriginal, annoCopy);
		}

		@Override
		public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
			super.visitTryCatchBlock(start, end, handler, type);
			copy.visitTryCatchBlock(clone(start), clone(end), handler, type);
		}

		@Override
		public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			AnnotationVisitor annoOriginal = super.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
			AnnotationVisitor annoCopy = copy.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
			return new CopyingAnnotationVisitor(annoOriginal, annoCopy);
		}

		@Override
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
			super.visitLocalVariable(name, desc, signature, start, end, index);
			copy.visitLocalVariable(name, desc, signature, clone(start), clone(end), index);
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath,
															  Label[] start, Label[] end, int[] idx,
															  String desc, boolean visible) {
			AnnotationVisitor annoOriginal =
					super.visitLocalVariableAnnotation(typeRef, typePath, start, end, idx, desc, visible);
			AnnotationVisitor annoCopy =
					copy.visitLocalVariableAnnotation(typeRef, typePath, clone(start), clone(end), idx, desc, visible);
			return new CopyingAnnotationVisitor(annoOriginal, annoCopy);
		}

		@Override
		public void visitLineNumber(int line, Label start) {
			super.visitLineNumber(line, start);
			copy.visitLineNumber(line, clone(start));
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			super.visitMaxs(maxStack, maxLocals);
			copy.visitMaxs(maxStack, maxLocals);
		}

		private Label clone(Label label) {
			return labelMap.computeIfAbsent(label, l -> new Label());
		}

		private Label[] clone(Label[] labels) {
			Label[] array = new Label[labels.length];
			for (int i = 0; i < labels.length; i++)
				array[i] = clone(labels[i]);
			return array;
		}
	}

	private static class CopyingAnnotationVisitor extends AnnotationVisitor {
		private final AnnotationVisitor copy;

		public CopyingAnnotationVisitor(AnnotationVisitor original, AnnotationVisitor copy) {
			super(RecafConstants.getAsmVersion(), original);
			this.copy = copy;
		}

		@Override
		public void visit(String name, Object value) {
			super.visit(name, value);
			copy.visit(name, value);
		}

		@Override
		public void visitEnum(String name, String desc, String value) {
			super.visitEnum(name, desc, value);
			copy.visitEnum(name, desc, value);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String desc) {
			AnnotationVisitor annoOriginal = super.visitAnnotation(name, desc);
			AnnotationVisitor annoCopy = copy.visitAnnotation(name, desc);
			return new CopyingAnnotationVisitor(annoOriginal, annoCopy);
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			AnnotationVisitor annoOriginal = super.visitArray(name);
			AnnotationVisitor annoCopy = copy.visitArray(name);
			return new CopyingAnnotationVisitor(annoOriginal, annoCopy);
		}
	}
}
