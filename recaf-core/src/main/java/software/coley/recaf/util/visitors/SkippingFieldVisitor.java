package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.TypePath;
import software.coley.recaf.RecafConstants;

/**
 * Field visitor that skips over all content by default.
 *
 * @author Matt Coley
 */
public class SkippingFieldVisitor extends FieldVisitor {
	public SkippingFieldVisitor() {
		this(null);
	}

	public SkippingFieldVisitor(@Nullable FieldVisitor fv) {
		super(RecafConstants.getAsmVersion(), fv);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return null;
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		return null;
	}

	@Override
	public void visitAttribute(Attribute attribute) {
		// no-op
	}

	@Override
	public void visitEnd() {
		// no-op
	}
}
