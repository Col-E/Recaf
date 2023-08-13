package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.*;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;

/**
 * Simple visitor for removing an annotation.
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

	/**
	 * @param cv
	 * 		Visitor of a class.
	 * @param annotationType
	 * 		Annotation type to remove on a method.
	 * @param method
	 * 		Method to target, or {@code null} for any method.
	 *
	 * @return Visitor that removes method annotations in the requested circumstances.
	 */
	@Nonnull
	public static ClassVisitor forClass(@Nonnull ClassVisitor cv, @Nonnull String annotationType, @Nullable MethodMember method) {
		return new ClassVisitor(RecafConstants.getAsmVersion(), cv) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
				if (method == null || (method.getName().equals(name) && method.getDescriptor().equals(descriptor)))
					return new MethodAnnotationRemovingVisitor(mv, annotationType);
				return mv;
			}
		};
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