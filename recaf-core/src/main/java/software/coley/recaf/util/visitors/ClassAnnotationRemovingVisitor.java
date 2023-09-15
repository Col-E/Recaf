package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.TypePath;
import software.coley.recaf.RecafConstants;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple visitor for inserting a annotation.
 *
 * @author Matt Coley
 */
public class ClassAnnotationRemovingVisitor extends ClassVisitor {
	private final Set<String> annotationTypes;

	/**
	 * @param cv
	 * 		Parent visitor.
	 * @param annotationType
	 * 		Annotation type to remove.
	 */
	public ClassAnnotationRemovingVisitor(@Nullable ClassVisitor cv,
										  @Nonnull String annotationType) {
		this(cv, Collections.singleton(annotationType));
	}

	/**
	 * @param cv
	 * 		Parent visitor.
	 * @param annotationTypes
	 * 		Annotation types to remove.
	 */
	public ClassAnnotationRemovingVisitor(@Nullable ClassVisitor cv,
										  @Nonnull Collection<String> annotationTypes) {
		super(RecafConstants.getAsmVersion(), cv);
		this.annotationTypes = new HashSet<>(annotationTypes);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (annotationTypes.contains(descriptor.substring(1, descriptor.length() - 1)))
			return null;
		return super.visitAnnotation(descriptor, visible);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		if (annotationTypes.contains(descriptor.substring(1, descriptor.length() - 1)))
			return null;
		return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
	}
}