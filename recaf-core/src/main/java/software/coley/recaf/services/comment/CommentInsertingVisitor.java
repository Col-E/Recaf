package software.coley.recaf.services.comment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;

/**
 * A class visitor which inserts {@link CommentKey} annotations into the given class.
 *
 * @author Matt Coley
 */
public class CommentInsertingVisitor extends ClassVisitor {
	private final ClassComments comments;
	private final ClassPathNode classPath;
	private int insertions;

	/**
	 * @param comments
	 * 		Comment container for the class.
	 * @param classPath
	 * 		Path to class in its containing workspace.
	 * @param cv
	 * 		Delegate class-visitor.
	 */
	public CommentInsertingVisitor(@Nonnull ClassComments comments, @Nonnull ClassPathNode classPath, @Nullable ClassVisitor cv) {
		super(RecafConstants.getAsmVersion(), cv);
		this.comments = comments;
		this.classPath = classPath;
	}

	/**
	 * @return Number of inserted comment annotations.
	 */
	public int getInsertions() {
		return insertions;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);

		// Insert key for comment
		String comment = comments.getClassComment();
		if (comment != null) {
			CommentKey key = CommentKey.id(classPath);
			visitAnnotation(key.annotationDescriptor(), true);
			insertions++;
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);

		// Insert key for comment
		String comment = comments.getFieldComment(name, descriptor);
		if (comment != null) {
			FieldMember field = classPath.getValue().getDeclaredField(name, descriptor);
			if (field != null) {
				CommentKey key = CommentKey.id(classPath.child(field));
				fv.visitAnnotation(key.annotationDescriptor(), true);
				insertions++;
			}
		}

		return fv;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

		// Insert key for comment
		String comment = comments.getMethodComment(name, descriptor);
		if (comment != null) {
			MethodMember method = classPath.getValue().getDeclaredMethod(name, descriptor);
			if (method != null) {
				CommentKey key = CommentKey.id(classPath.child(method));
				mv = new CommentAppender(mv, key);
				insertions++;
			}
		}

		return mv;
	}

	/**
	 * This class exists to facilitate optimal use of {@link ClassWriter#ClassWriter(ClassReader, int)}
	 * <i>(See: {@code MethodWriter#canCopyMethodAttributes(ClassReader, boolean, boolean, int, int, int)})</i>.
	 * <br>
	 * Methods are copied as-is unless there is a custom subtype of {@link MethodVisitor} used, hence this existing.
	 * Any methods that aren't having comments inserted then get copied over much faster than if they were rebuilt
	 * from scratch.
	 */
	private static class CommentAppender extends MethodVisitor {
		private final CommentKey key;

		private CommentAppender(@Nullable MethodVisitor mv, @Nonnull CommentKey key) {
			super(RecafConstants.getAsmVersion(), mv);
			this.key = key;
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			visitAnnotation(key.annotationDescriptor(), true);
		}
	}
}
