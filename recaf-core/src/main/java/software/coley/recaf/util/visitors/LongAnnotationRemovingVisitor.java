package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.*;
import software.coley.recaf.RecafConstants;

/**
 * A visitor that strips long named annotations from classes.
 *
 * @author Matt Coley
 */
public class LongAnnotationRemovingVisitor extends ClassVisitor {
	private final int maxAllowedLength;

	/**
	 * @param cv
	 * 		Parent visitor.
	 */
	public LongAnnotationRemovingVisitor(@Nullable ClassVisitor cv, int maxAllowedLength) {
		super(RecafConstants.getAsmVersion(), cv);
		this.maxAllowedLength = maxAllowedLength;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
		return new FieldDupAnnoRemover(fv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new MethodDupAnnoRemover(mv);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (descriptor.length() < maxAllowedLength)
			return super.visitAnnotation(descriptor, visible);
		return null;
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		if (descriptor.length() < maxAllowedLength)
			return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
		return null;
	}

	private class FieldDupAnnoRemover extends FieldVisitor {
		protected FieldDupAnnoRemover(@Nullable FieldVisitor fv) {
			super(RecafConstants.getAsmVersion(), fv);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength) return super.visitAnnotation(descriptor, visible);
			return null;
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength)
				return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
			return null;
		}
	}

	private class MethodDupAnnoRemover extends MethodVisitor {
		protected MethodDupAnnoRemover(@Nullable MethodVisitor mv) {
			super(RecafConstants.getAsmVersion(), mv);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength) return super.visitAnnotation(descriptor, visible);
			return null;
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength)
				return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
			return null;
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength)
				return super.visitParameterAnnotation(parameter, descriptor, visible);
			return null;
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength)
				return super.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
			return null;
		}

		@Override
		public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength)
				return super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
			return null;
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength)
				return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
			return null;
		}
	}
}
