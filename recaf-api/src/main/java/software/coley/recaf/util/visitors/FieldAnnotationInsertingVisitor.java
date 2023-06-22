package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import software.coley.recaf.RecafConstants;

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

	@Override
	public void visitEnd() {
		visitAnnotation(inserted.desc, true);
		super.visitEnd();
	}
}