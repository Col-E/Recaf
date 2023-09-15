package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import software.coley.recaf.RecafConstants;

/**
 * Simple visitor for inserting a annotation.
 *
 * @author Matt Coley
 */
public class ClassAnnotationInsertingVisitor extends ClassVisitor {
	private final AnnotationNode inserted;

	/**
	 * @param cv
	 * 		Parent visitor.
	 * @param inserted
	 * 		Annotation to insert.
	 */
	public ClassAnnotationInsertingVisitor(@Nullable ClassVisitor cv,
										   @Nonnull AnnotationNode inserted) {
		super(RecafConstants.getAsmVersion(), cv);
		this.inserted = inserted;
	}

	@Override
	public void visitEnd() {
		visitAnnotation(inserted.desc, true);
		super.visitEnd();
	}
}