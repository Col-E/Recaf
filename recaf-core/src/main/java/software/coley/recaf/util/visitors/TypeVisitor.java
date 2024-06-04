package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.*;
import org.slf4j.Logger;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.Logging;

import java.util.function.Consumer;

/**
 * Visitor to accept top-level types of referenced fields, methods, annotations, and nest mates.
 *
 * @author Matt Coley
 */
public class TypeVisitor extends ClassVisitor {
	private static final Logger logger = Logging.get(TypeVisitor.class);
	private final Consumer<Type> typeConsumer;

	/**
	 * @param typeConsumer
	 * 		Type consumer to accept seen types.
	 * 		The same type may be visited multiple times.
	 * 		Method types are also passed in.
	 */
	public TypeVisitor(@Nonnull Consumer<Type> typeConsumer) {
		super(RecafConstants.getAsmVersion());
		this.typeConsumer = typeConsumer;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (interfaces != null)
			for (String exception : interfaces)
				acceptType(exception);
		acceptType(superName);
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
		acceptDescriptor(descriptor);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		acceptDescriptor(descriptor);
		return null;
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		acceptDescriptor(descriptor);
		return null;
	}

	@Override
	public void visitAttribute(Attribute attribute) {
		// no-op
	}

	@Override
	public void visitNestMember(String nestMember) {
		acceptType(nestMember);
	}

	@Override
	public void visitPermittedSubclass(String permittedSubclass) {
		acceptType(permittedSubclass);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		// no-op
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
		acceptDescriptor(descriptor);
		return null;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		acceptDescriptor(descriptor);
		return null;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		acceptDescriptor(descriptor);
		if (exceptions != null)
			for (String exception : exceptions)
				acceptType(exception);
		return null;
	}

	private void acceptType(@Nullable String internalName) {
		if (internalName == null || internalName.isEmpty()) return;

		try {
			Type methodType = Type.getObjectType(internalName);
			typeConsumer.accept(methodType);
		} catch (Throwable t) {
			logger.trace("Ignored invalid internal name: {}", internalName, t);
		}
	}

	private void acceptDescriptor(@Nullable String descriptor) {
		if (descriptor == null || descriptor.isEmpty()) return;

		try {
			if (descriptor.charAt(0) == '(') {
				Type methodType = Type.getMethodType(descriptor);
				typeConsumer.accept(methodType);
			} else {
				Type type = Type.getType(descriptor);
				typeConsumer.accept(type);
			}
		} catch (Throwable t) {
			logger.trace("Ignored invalid type: {}", descriptor, t);
		}
	}
}
