package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;
import software.coley.recaf.RecafConstants;

/**
 * A visitor that strips long named annotations from classes.
 *
 * @author Matt Coley
 */
public class LongAnnotationRemovingVisitor extends ClassVisitor {
	private final int maxAllowedLength;
	private boolean detected;

	/**
	 * @param cv
	 * 		Parent visitor.
	 * @param maxAllowedLength
	 * 		Max length of allowed annotation descriptors.
	 */
	public LongAnnotationRemovingVisitor(@Nullable ClassVisitor cv, int maxAllowedLength) {
		super(RecafConstants.getAsmVersion(), cv);
		this.maxAllowedLength = maxAllowedLength;
	}

	/**
	 * @return {@code true} if any long annotations were removed.
	 */
	public boolean hasDetectedLongAnnotations() {
		return detected;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
		return new FieldLongAnnoRemover(fv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new LongMethodAnnoRemover(mv);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (descriptor.length() < maxAllowedLength)
			return new LongSubAnnoRemover(super.visitAnnotation(descriptor, visible));

		detected = true;
		return null;
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		if (descriptor.length() < maxAllowedLength)
			return new LongSubAnnoRemover(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible));

		detected = true;
		return null;
	}

	private class LongSubAnnoRemover extends AnnotationVisitor {
		public LongSubAnnoRemover(@Nullable AnnotationVisitor av) {
			super(RecafConstants.getAsmVersion(), av);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String descriptor) {
			if (descriptor.length() < maxAllowedLength)
				return new LongSubAnnoRemover(super.visitAnnotation(name, descriptor));

			detected = true;
			return null;
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			return new LongSubAnnoRemover(super.visitArray(name));
		}
	}

	private class FieldLongAnnoRemover extends FieldVisitor {
		protected FieldLongAnnoRemover(@Nullable FieldVisitor fv) {
			super(RecafConstants.getAsmVersion(), fv);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength)
				return new LongSubAnnoRemover(super.visitAnnotation(descriptor, visible));

			detected = true;
			return null;
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength)
				return new LongSubAnnoRemover(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible));

			detected = true;
			return null;
		}
	}

	private class LongMethodAnnoRemover extends MethodVisitor {
		protected LongMethodAnnoRemover(@Nullable MethodVisitor mv) {
			super(RecafConstants.getAsmVersion(), mv);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength)
				return new LongSubAnnoRemover(super.visitAnnotation(descriptor, visible));

			detected = true;
			return null;
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength)
				return new LongSubAnnoRemover(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible));

			detected = true;
			return null;
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength)
				return new LongSubAnnoRemover(super.visitParameterAnnotation(parameter, descriptor, visible));

			detected = true;
			return null;
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength)
				return new LongSubAnnoRemover(super.visitInsnAnnotation(typeRef, typePath, descriptor, visible));

			detected = true;
			return null;
		}

		@Override
		public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength)
				return new LongSubAnnoRemover(super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible));

			detected = true;
			return null;
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
			if (descriptor.length() < maxAllowedLength)
				return new LongSubAnnoRemover(super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible));

			detected = true;
			return null;
		}
	}
}
