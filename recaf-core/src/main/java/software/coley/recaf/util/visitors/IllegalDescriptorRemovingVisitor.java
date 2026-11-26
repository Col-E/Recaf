package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.util.Types;

/**
 * Visitor that removes fields and methods with invalid descriptors.
 *
 * @author Matt Coley
 */
public class IllegalDescriptorRemovingVisitor extends ClassVisitor {
	/**
	 * @param cv
	 * 		Parent visitor.
	 */
	public IllegalDescriptorRemovingVisitor(@Nullable ClassVisitor cv) {
		super(RecafConstants.getAsmVersion(), cv);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (!Types.isValidFieldDesc(desc))
			return null;
		AnnotationVisitor av = super.visitAnnotation(desc, visible);
		return new InvalidAnnotationRemovingVisitor(av);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		if (!Types.isValidFieldDesc(desc))
			return null;
		AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, desc, visible);
		return new InvalidAnnotationRemovingVisitor(av);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if (!Types.isValidFieldDesc(desc))
			return null;
		FieldVisitor fv = super.visitField(access, name, desc, signature, value);
		return new FieldVisitor(RecafConstants.getAsmVersion(), fv) {
			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				if (!Types.isValidFieldDesc(descriptor))
					return null;
				AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
				return new InvalidAnnotationRemovingVisitor(av);
			}

			@Override
			public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
				if (!Types.isValidFieldDesc(descriptor))
					return null;
				AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
				return new InvalidAnnotationRemovingVisitor(av);
			}
		};
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (!Types.isValidMethodDesc(desc))
			return null;
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		return new MethodVisitor(RecafConstants.getAsmVersion(), mv) {
			@Override
			public void visitLocalVariable(String localName, String localDesc, String localSignature, Label start, Label end, int index) {
				if (Types.isValidFieldDesc(localDesc))
					super.visitLocalVariable(localName, localDesc, localSignature, start, end, index);
			}

			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				if (!Types.isValidFieldDesc(descriptor))
					return null;
				AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
				return new InvalidAnnotationRemovingVisitor(av);
			}

			@Override
			public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
				if (!Types.isValidFieldDesc(descriptor))
					return null;
				AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
				return new InvalidAnnotationRemovingVisitor(av);
			}
		};
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String desc, String signature) {
		if (!Types.isValidFieldDesc(desc))
			return null;
		RecordComponentVisitor rcv = super.visitRecordComponent(name, desc, signature);
		return new RecordComponentVisitor(RecafConstants.getAsmVersion(), rcv) {
			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				if (!Types.isValidFieldDesc(descriptor))
					return null;
				AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
				return new InvalidAnnotationRemovingVisitor(av);
			}

			@Override
			public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
				if (!Types.isValidFieldDesc(descriptor))
					return null;
				AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
				return new InvalidAnnotationRemovingVisitor(av);
			}
		};
	}

	private static final class InvalidAnnotationRemovingVisitor extends AnnotationVisitor {
		public InvalidAnnotationRemovingVisitor(@Nullable AnnotationVisitor av) {
			super(RecafConstants.getAsmVersion(), av);
		}

		@Override
		public void visit(String name, Object value) {
			if (value instanceof Type type && !Types.isValidFieldDesc(type.getDescriptor()))
				return;
			super.visit(name, value);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String descriptor) {
			if (!Types.isValidFieldDesc(descriptor))
				return null;
			return new InvalidAnnotationRemovingVisitor(super.visitAnnotation(name, descriptor));
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			return new InvalidAnnotationRemovingVisitor(super.visitArray(name));
		}
	}
}
