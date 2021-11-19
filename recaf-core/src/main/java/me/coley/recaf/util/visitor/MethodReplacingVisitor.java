package me.coley.recaf.util.visitor;

import me.coley.recaf.RecafConstants;
import me.coley.recaf.code.MemberInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

/**
 * Simple visitor for replacing a matched {@link MemberInfo}.
 *
 * @author Matt Coley
 */
public class MethodReplacingVisitor extends ClassVisitor {
	private final MemberInfo memberInfo;
	private final MethodNode replacementMethod;
	private boolean replaced;

	/**
	 * @param cv
	 * 		Parent visitor where the removal will be applied in.
	 * @param memberInfo
	 * 		Details of the method to replace.
	 * @param replacementMethod
	 * 		Method to replace with.
	 */
	public MethodReplacingVisitor(ClassVisitor cv, MemberInfo memberInfo, MethodNode replacementMethod) {
		super(RecafConstants.ASM_VERSION, cv);
		this.memberInfo = memberInfo;
		this.replacementMethod = replacementMethod;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, sig, exceptions);
		if (memberInfo.getName().equals(name) && memberInfo.getDescriptor().equals(desc)) {
			replaced = true;
			replacementMethod.accept(mv);
			return null;
		}
		return mv;
	}

	/**
	 * @return {@code true} when the method was replaced.
	 */
	public boolean isReplaced() {
		return replaced;
	}
}
