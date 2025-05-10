package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.TypePath;
import software.coley.recaf.RecafConstants;

/**
 * Class visitor that skips over all content by default.
 *
 * @author Matt Coley
 */
public class SkippingClassVisitor extends ClassVisitor {
	public SkippingClassVisitor() {
		this(null);
	}

	public SkippingClassVisitor(@Nullable ClassVisitor cv) {
		super(RecafConstants.getAsmVersion(), cv);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		// no-op
	}

	@Override
	public void visitSource(String source, String debug) {
		// no-op
	}

	@Override
	public ModuleVisitor visitModule(String name, int access, String version) {
		return null;
	}

	@Override
	public void visitNestHost(String nestHost) {
		// no-op
	}

	@Override
	public void visitOuterClass(String owner, String name, String descriptor) {
		// no-op
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
	public void visitNestMember(String nestMember) {
		// no-op
	}

	@Override
	public void visitPermittedSubclass(String permittedSubclass) {
		// no-op
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		// no-op
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
		return null;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		return null;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return null;
	}

	@Override
	public void visitEnd() {
		// no-op
	}
}
