package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import software.coley.recaf.RecafConstants;

/**
 * Simple visitor for inserting an annotation.
 *
 * @author Matt Coley
 */
public class MethodAnnotationInsertingVisitor extends MethodVisitor {
	private final AnnotationNode inserted;

	/**
	 * @param mv
	 * 		Parent visitor.
	 * @param inserted
	 * 		Annotation to insert.
	 */
	public MethodAnnotationInsertingVisitor(@Nullable MethodVisitor mv,
											@Nonnull AnnotationNode inserted) {
		super(RecafConstants.getAsmVersion(), mv);
		this.inserted = inserted;
	}

	@Override
	public void visitEnd() {
		visitAnnotation(inserted.desc, true);
		super.visitEnd();
	}
}