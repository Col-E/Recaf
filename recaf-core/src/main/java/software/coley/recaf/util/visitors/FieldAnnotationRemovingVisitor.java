package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.TypePath;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.member.FieldMember;

/**
 * Simple visitor for removing an annotation.
 *
 * @author Matt Coley
 */
public class FieldAnnotationRemovingVisitor extends FieldVisitor {
	private final String annotationType;

	/**
	 * @param fv
	 * 		Parent visitor.
	 * @param annotationType
	 * 		Annotation type to remove.
	 */
	public FieldAnnotationRemovingVisitor(@Nullable FieldVisitor fv,
										  @Nonnull String annotationType) {
		super(RecafConstants.getAsmVersion(), fv);
		this.annotationType = annotationType;
	}

	/**
	 * @param cv
	 * 		Visitor of a class.
	 * @param annotationType
	 * 		Annotation type to remove on a field.
	 * @param field
	 * 		Field to target, or {@code null} for any field.
	 *
	 * @return Visitor that removes field annotations in the requested circumstances.
	 */
	@Nonnull
	public static ClassVisitor forClass(@Nonnull ClassVisitor cv, @Nonnull String annotationType, @Nullable FieldMember field) {
		return new ClassVisitor(RecafConstants.getAsmVersion(), cv) {
			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
				if (field == null || (field.getName().equals(name) && field.getDescriptor().equals(descriptor)))
					return new FieldAnnotationRemovingVisitor(fv, annotationType);
				return fv;
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
}