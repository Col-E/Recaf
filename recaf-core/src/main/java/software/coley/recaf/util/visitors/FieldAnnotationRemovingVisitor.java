package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.TypePath;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.member.FieldMember;

import java.util.Collection;
import java.util.Collections;

/**
 * Simple visitor for removing an annotation.
 *
 * @author Matt Coley
 */
public class FieldAnnotationRemovingVisitor extends FieldVisitor {
	private final Collection<String> annotationTypes;

	/**
	 * @param fv
	 * 		Parent visitor.
	 * @param annotationType
	 * 		Annotation type to remove.
	 */
	public FieldAnnotationRemovingVisitor(@Nullable FieldVisitor fv,
	                                      @Nonnull String annotationType) {
		this(fv, Collections.singleton(annotationType));
	}

	/**
	 * @param fv
	 * 		Parent visitor.
	 * @param annotationTypes
	 * 		Annotation types to remove.
	 */
	public FieldAnnotationRemovingVisitor(@Nullable FieldVisitor fv,
	                                      @Nonnull Collection<String> annotationTypes) {
		super(RecafConstants.getAsmVersion(), fv);
		this.annotationTypes = annotationTypes;
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
		return forClass(cv, Collections.singleton(annotationType), field);
	}

	/**
	 * @param cv
	 * 		Visitor of a class.
	 * @param annotationTypes
	 * 		Annotation types to remove on a field.
	 * @param field
	 * 		Field to target, or {@code null} for any field.
	 *
	 * @return Visitor that removes field annotations in the requested circumstances.
	 */
	@Nonnull
	public static ClassVisitor forClass(@Nonnull ClassVisitor cv, @Nonnull Collection<String> annotationTypes, @Nullable FieldMember field) {
		return new ClassVisitor(RecafConstants.getAsmVersion(), cv) {
			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
				if (field == null || (field.getName().equals(name) && field.getDescriptor().equals(descriptor)))
					return new FieldAnnotationRemovingVisitor(fv, annotationTypes);
				return fv;
			}
		};
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		String type = descriptor.substring(1, descriptor.length() - 1);
		if (annotationTypes.contains(type))
			return null;
		return super.visitAnnotation(descriptor, visible);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		String type = descriptor.substring(1, descriptor.length() - 1);
		if (annotationTypes.contains(type))
			return null;
		return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
	}
}