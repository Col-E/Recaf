package software.coley.recaf.util.visitors;


import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;

import static org.objectweb.asm.Opcodes.*;

/**
 * Visitor for adding a stubbed outline for a given member.
 *
 * @author Justus Garbe
 * @author Matt Coley
 */
public class MemberStubAddingVisitor extends ClassVisitor {
	private final ClassMember member;

	/**
	 * @param cv
	 * 		Parent visitor.
	 * @param member
	 * 		Member outline to visit.
	 */
	public MemberStubAddingVisitor(@Nullable ClassVisitor cv, @Nonnull ClassMember member) {
		super(RecafConstants.getAsmVersion(), cv);
		this.member = member;
	}

	@Override
	public void visitEnd() {
		super.visitEnd();

		int access = member.getAccess();
		String name = member.getName();
		String desc = member.getDescriptor();
		String signature = member.getSignature();

		if (member instanceof FieldMember fm) {
			Object defaultValue = fm.getDefaultValue();
			visitField(access, name, desc, signature, defaultValue).visitEnd();
		} else if (member instanceof MethodMember mm) {
			String[] exceptions = mm.getThrownTypes().toArray(new String[0]);

			MethodVisitor mv = visitMethod(access, name, desc, signature, exceptions);
			if (!mm.hasAbstractModifier() || !mm.hasNativeModifier()) {
				Label start = new Label();
				Label end = new Label();

				mv.visitLabel(start);
				mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
				mv.visitInsn(DUP);
				mv.visitLdcInsn("TODO: Implementation");
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false);
				mv.visitInsn(ATHROW);
				mv.visitLabel(end);

				for (LocalVariable lv : mm.getLocalVariables()) {
					mv.visitLocalVariable(lv.getName(), lv.getDescriptor(), lv.getSignature(),
							start, end, lv.getIndex());
				}
			}
			mv.visitEnd();
		}
	}
}
