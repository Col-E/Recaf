package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;
import software.coley.recaf.RecafConstants;

/**
 * Simple visitor for inserting a annotation.
 *
 * @author Matt Coley
 */
public class MethodAnnotationRemovingVisitor extends MethodVisitor {
	private final String annotationType;

	/**
	 * @param mv
	 * 		Parent visitor.
	 * @param annotationType
	 * 		Annotation type to remove.
	 */
	public MethodAnnotationRemovingVisitor(@Nullable MethodVisitor mv,
										   @Nonnull String annotationType) {
		super(RecafConstants.getAsmVersion(), mv);
		this.annotationType = annotationType;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (annotationType.equals(descriptor.substring(1, descriptor.length() - 1)))
			return null;
		return super.visitAnnotation(descriptor, visible);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		if (annotationType.equals(descriptor.substring(1, descriptor.length() - 1)))
			return null;
		return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
		if (annotationType.equals(descriptor.substring(1, descriptor.length() - 1)))
			return null;
		return super.visitParameterAnnotation(parameter, descriptor, visible);
	}

	@Override
	public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		if (annotationType.equals(descriptor.substring(1, descriptor.length() - 1)))
			return null;
		return super.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
	}

	@Override
	public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		if (annotationType.equals(descriptor.substring(1, descriptor.length() - 1)))
			return null;
		return super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
	}

	@Override
	public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
		if (annotationType.equals(descriptor.substring(1, descriptor.length() - 1)))
			return null;
		return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
	}
}