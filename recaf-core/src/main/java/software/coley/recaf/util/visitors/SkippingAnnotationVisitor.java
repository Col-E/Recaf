package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import software.coley.recaf.RecafConstants;

/**
 * Annotation visitor that skips over all content by default.
 *
 * @author Matt Coley
 */
public class SkippingAnnotationVisitor extends AnnotationVisitor {
	public SkippingAnnotationVisitor() {
		this(null);
	}

	public SkippingAnnotationVisitor(@Nullable AnnotationVisitor av) {
		super(RecafConstants.getAsmVersion(), av);
	}

	@Override
	public void visit(String name, Object value) {
		// no-op
	}

	@Override
	public void visitEnum(String name, String descriptor, String value) {
		// no-op
	}

	@Override
	public AnnotationVisitor visitAnnotation(String name, String descriptor) {
		return null;
	}

	@Override
	public AnnotationVisitor visitArray(String name) {
		return null;
	}

	@Override
	public void visitEnd() {
		// no-op
	}
}
