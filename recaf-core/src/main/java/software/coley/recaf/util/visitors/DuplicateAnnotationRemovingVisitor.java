package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.TypePath;
import software.coley.recaf.RecafConstants;

import java.util.HashSet;
import java.util.Set;

/**
 * A visitor that strips duplicate annotation usage from classes.
 *
 * @author Matt Coley
 */
public class DuplicateAnnotationRemovingVisitor extends ClassVisitor {
	private final Set<String> cAnnosVisited = new HashSet<>();
	private final Set<TypeAnnoInfo> cTypeAnnosVisited = new HashSet<>();
	private boolean detected;

	/**
	 * @param cv
	 * 		Parent visitor.
	 */
	public DuplicateAnnotationRemovingVisitor(@Nullable ClassVisitor cv) {
		super(RecafConstants.getAsmVersion(), cv);
	}

	/**
	 * @return {@code true} if any duplicate annotations were removed.
	 */
	public boolean hasDetectedDuplicateAnnotations() {
		return detected;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
		return new FieldDupAnnoRemover(fv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new MethodDupAnnoRemover(mv);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (cAnnosVisited.add(descriptor))
			return super.visitAnnotation(descriptor, visible);

		detected = true;
		return null;
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		if (cTypeAnnosVisited.add(new TypeAnnoInfo(typeRef, descriptor)))
			return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);

		detected = true;
		return null;
	}

	private class FieldDupAnnoRemover extends FieldVisitor {
		private final Set<String> fAnnosVisited = new HashSet<>();
		private final Set<TypeAnnoInfo> fTypeAnnosVisited = new HashSet<>();

		protected FieldDupAnnoRemover(@Nullable FieldVisitor fv) {
			super(RecafConstants.getAsmVersion(), fv);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (fAnnosVisited.add(descriptor))
				return super.visitAnnotation(descriptor, visible);

			detected = true;
			return null;
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (fTypeAnnosVisited.add(new TypeAnnoInfo(typeRef, descriptor)))
				return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);

			detected = true;
			return null;
		}
	}

	private class MethodDupAnnoRemover extends MethodVisitor {
		private final Set<String> mAnnosVisited = new HashSet<>();
		private final Set<TypeAnnoInfo> mTypeAnnosVisited = new HashSet<>();
		private final Set<TypeAnnoInfo> mInsnTypeAnnosVisited = new HashSet<>();
		private final Set<TypeAnnoInfo> mTryTypeAnnosVisited = new HashSet<>();
		private final Set<TypeAnnoInfo> mVarTypeAnnosVisited = new HashSet<>();
		private final Set<ParamAnnoInfo> mParamAnnosVisited = new HashSet<>();

		protected MethodDupAnnoRemover(@Nullable MethodVisitor mv) {
			super(RecafConstants.getAsmVersion(), mv);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (mAnnosVisited.add(descriptor))
				return super.visitAnnotation(descriptor, visible);

			detected = true;
			return null;
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (mTypeAnnosVisited.add(new TypeAnnoInfo(typeRef, descriptor)))
				return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);

			detected = true;
			return null;
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
			if (mParamAnnosVisited.add(new ParamAnnoInfo(parameter, descriptor)))
				return super.visitParameterAnnotation(parameter, descriptor, visible);

			detected = true;
			return null;
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (mInsnTypeAnnosVisited.add(new TypeAnnoInfo(typeRef, descriptor)))
				return super.visitInsnAnnotation(typeRef, typePath, descriptor, visible);

			detected = true;
			return null;
		}

		@Override
		public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (mTryTypeAnnosVisited.add(new TypeAnnoInfo(typeRef, descriptor)))
				return super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);

			detected = true;
			return null;
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
			if (mVarTypeAnnosVisited.add(new TypeAnnoInfo(typeRef, descriptor)))
				return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);

			detected = true;
			return null;
		}
	}

	private class RecordDupAnnoRemover extends RecordComponentVisitor {
		private final Set<String> rAnnosVisited = new HashSet<>();
		private final Set<TypeAnnoInfo> rTypeAnnosVisited = new HashSet<>();

		protected RecordDupAnnoRemover(@Nullable RecordComponentVisitor rv) {
			super(RecafConstants.getAsmVersion(), rv);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (rAnnosVisited.add(descriptor))
				return super.visitAnnotation(descriptor, visible);

			detected = true;
			return null;
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			if (rTypeAnnosVisited.add(new TypeAnnoInfo(typeRef, descriptor)))
				return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);

			detected = true;
			return null;
		}
	}

	private record ParamAnnoInfo(int param, String desc) {
	}

	private record TypeAnnoInfo(int typeRef, String desc) {
	}
}
