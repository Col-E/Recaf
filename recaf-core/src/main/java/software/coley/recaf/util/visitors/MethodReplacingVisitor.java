package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.member.MethodMember;

/**
 * Simple visitor for replacing a matched {@link MethodMember}.
 *
 * @author Matt Coley
 */
public class MethodReplacingVisitor extends ClassVisitor {
	private final MethodMember methodMember;
	private final MethodNode replacementMethod;
	private boolean replaced;

	/**
	 * @param cv
	 * 		Parent visitor where the removal will be applied in.
	 * @param methodMember
	 * 		Details of the method to replace.
	 * @param replacementMethod
	 * 		Method to replace with.
	 */
	public MethodReplacingVisitor(@Nullable ClassVisitor cv,
								  @Nonnull MethodMember methodMember,
								  @Nonnull MethodNode replacementMethod) {
		super(RecafConstants.getAsmVersion(), cv);
		this.methodMember = methodMember;
		this.replacementMethod = replacementMethod;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
		if (methodMember.getName().equals(name) && methodMember.getDescriptor().equals(desc)) {
			// Update from the replacement method
			access = replacementMethod.access;
			name = replacementMethod.name;
			desc = replacementMethod.desc;
			sig = replacementMethod.signature;
			exceptions = replacementMethod.exceptions.toArray(new String[0]);
			// Visit
			MethodVisitor mv = super.visitMethod(access, name, desc, sig, exceptions);
			replaced = true;
			replacementMethod.accept(mv);
			return null;
		}
		return super.visitMethod(access, name, desc, sig, exceptions);
	}

	/**
	 * @return {@code true} when the method was replaced.
	 */
	public boolean isReplaced() {
		return replaced;
	}
}
