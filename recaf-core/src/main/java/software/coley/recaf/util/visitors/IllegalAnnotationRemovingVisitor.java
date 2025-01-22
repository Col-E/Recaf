package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.util.Types;

/**
 * A visitor that strips illegally named annotations from classes.
 *
 * @author Matt Coley
 */
public class IllegalAnnotationRemovingVisitor extends ClassVisitor {
	private boolean detected;

	/**
	 * @param cv
	 * 		Parent visitor.
	 */
	public IllegalAnnotationRemovingVisitor(@Nullable ClassVisitor cv) {
		super(RecafConstants.getAsmVersion(), cv);
	}

	/**
	 * @return {@code true} when illegal annotations have been removed from the visited class.
	 */
	public boolean hasDetectedIllegalAnnotations() {
		return detected;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
		return new FieldIllegalAnnoRemover(fv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new MethodIllegalAnnoRemover(mv);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (!isValidAnnotationDesc(descriptor))
			return null;
		return new IllegalSubAnnoRemover(super.visitAnnotation(descriptor, visible));
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		if (!isValidAnnotationDesc(descriptor))
			return null;
		return new IllegalSubAnnoRemover(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible));
	}

	private boolean isValidAnnotationDesc(@Nullable String descriptor) {
		boolean valid = isValidAnnotationDesc0(descriptor);
		if (!valid)
			detected = true;
		return valid;
	}

	private static boolean isValidAnnotationDesc0(@Nullable String descriptor) {
		if (descriptor == null || descriptor.isBlank())
			return false; // Must not be empty
		char c = descriptor.charAt(0);
		if (c == 'V' || c == '(')
			return false; // Must not be void or method type
		return Types.isValidDesc(descriptor);
	}

	private class IllegalSubAnnoRemover extends AnnotationVisitor {
		protected IllegalSubAnnoRemover(@Nullable AnnotationVisitor av) {
			super(RecafConstants.getAsmVersion(), av);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String descriptor) {
			if (!isValidAnnotationDesc(descriptor))
				return null;
			return new IllegalSubAnnoRemover(super.visitAnnotation(name, descriptor));
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			return new IllegalSubAnnoRemover(super.visitArray(name));
		}
	}

	private class FieldIllegalAnnoRemover extends FieldVisitor {
		protected FieldIllegalAnnoRemover(@Nullable FieldVisitor fv) {
			super(RecafConstants.getAsmVersion(), fv);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (!isValidAnnotationDesc(descriptor))
				return null;
			return new IllegalSubAnnoRemover(super.visitAnnotation(descriptor, visible));
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (!isValidAnnotationDesc(descriptor))
				return null;
			return new IllegalSubAnnoRemover(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible));
		}
	}

	private class MethodIllegalAnnoRemover extends MethodVisitor {
		protected MethodIllegalAnnoRemover(@Nullable MethodVisitor mv) {
			super(RecafConstants.getAsmVersion(), mv);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (!isValidAnnotationDesc(descriptor))
				return null;
			return new IllegalSubAnnoRemover(super.visitAnnotation(descriptor, visible));
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (!isValidAnnotationDesc(descriptor))
				return null;
			return new IllegalSubAnnoRemover(super.visitTypeAnnotation(typeRef, typePath, descriptor, visible));
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
			if (!isValidAnnotationDesc(descriptor))
				return null;
			return new IllegalSubAnnoRemover(super.visitParameterAnnotation(parameter, descriptor, visible));
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (!isValidAnnotationDesc(descriptor))
				return null;
			return new IllegalSubAnnoRemover(super.visitInsnAnnotation(typeRef, typePath, descriptor, visible));
		}

		@Override
		public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (!isValidAnnotationDesc(descriptor))
				return null;
			return new IllegalSubAnnoRemover(super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible));
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
			if (!isValidAnnotationDesc(descriptor))
				return null;
			return new IllegalSubAnnoRemover(super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible));
		}
	}
}
