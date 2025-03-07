package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.member.FieldMember;

/**
 * Simple visitor for inserting an annotation.
 *
 * @author Matt Coley
 */
public class FieldAnnotationInsertingVisitor extends FieldVisitor {
	private final AnnotationNode inserted;

	/**
	 * @param fv
	 * 		Parent visitor.
	 * @param inserted
	 * 		Annotation to insert.
	 */
	public FieldAnnotationInsertingVisitor(@Nullable FieldVisitor fv,
										   @Nonnull AnnotationNode inserted) {
		super(RecafConstants.getAsmVersion(), fv);
		this.inserted = inserted;
	}

	/**
	 * @param cv
	 * 		Visitor of a class.
	 * @param inserted
	 * 		Annotation to insert on a field.
	 * @param field
	 * 		Field to target, or {@code null} for any field.
	 *
	 * @return Visitor that adds a field annotation in the requested circumstances.
	 */
	@Nonnull
	public static ClassVisitor forClass(@Nonnull ClassVisitor cv, @Nonnull AnnotationNode inserted, @Nullable FieldMember field) {
		return new ClassVisitor(RecafConstants.getAsmVersion(), cv) {
			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
				if (field == null || (field.getName().equals(name) && field.getDescriptor().equals(descriptor)))
					return new FieldAnnotationInsertingVisitor(fv, inserted);
				return fv;
			}
		};
	}

	@Override
	public void visitEnd() {
		visitAnnotation(inserted.desc, true);
		super.visitEnd();
	}
}