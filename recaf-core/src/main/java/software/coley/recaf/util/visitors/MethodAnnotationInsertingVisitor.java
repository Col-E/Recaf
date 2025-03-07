package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.member.MethodMember;

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

	/**
	 * @param cv
	 * 		Visitor of a class.
	 * @param inserted
	 * 		Annotation to insert on a method.
	 * @param method
	 * 		Method to target, or {@code null} for any method.
	 *
	 * @return Visitor that adds a method annotation in the requested circumstances.
	 */
	@Nonnull
	public static ClassVisitor forClass(@Nonnull ClassVisitor cv, @Nonnull AnnotationNode inserted, @Nullable MethodMember method) {
		return new ClassVisitor(RecafConstants.getAsmVersion(), cv) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
				if (method == null || (method.getName().equals(name) && method.getDescriptor().equals(descriptor)))
					return new MethodAnnotationInsertingVisitor(mv, inserted);
				return mv;
			}
		};
	}

	@Override
	public void visitEnd() {
		visitAnnotation(inserted.desc, true);
		super.visitEnd();
	}
}