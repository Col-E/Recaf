package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.util.AccessFlag;

/**
 * A visitor that removes method variables.
 * Generally useful for fixing kotlin classes since their variable tables are all sorts of wrong.
 *
 * @author Matt Coley
 */
public class MethodVariableRemovingVisitor extends ClassVisitor {
	private final MemberPredicate predicate;

	/**
	 * @param cv
	 * 		Parent visitor.
	 * @param predicate
	 * 		Predicate to match which methods will be cleaned, or {@code null} to clean all methods.
	 */
	public MethodVariableRemovingVisitor(@Nullable ClassVisitor cv, @Nullable MemberPredicate predicate) {
		super(RecafConstants.getAsmVersion(), cv);

		this.predicate = predicate;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

		// Skip if already no code.
		if (AccessFlag.isAbstract(access) || AccessFlag.isNative(access))
			return mv;

		// Only clean matched methods.
		if (predicate == null || predicate.matchMethod(access, name, descriptor, signature, exceptions))
			return new VarRemovingVisitor(mv);

		return mv;
	}

	/**
	 * Method visitor that removes any local variable debug info.
	 */
	public static class VarRemovingVisitor extends MethodVisitor {
		public VarRemovingVisitor(@Nullable MethodVisitor mv) {
			super(RecafConstants.getAsmVersion(), mv);
		}

		@Override
		public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
			// skip
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
			// skip
			return null;
		}
	}
}
