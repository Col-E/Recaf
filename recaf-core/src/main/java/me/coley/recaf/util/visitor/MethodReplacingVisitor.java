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
		super(RecafConstants.getAsmVersion(), cv);
		this.memberInfo = memberInfo;
		this.replacementMethod = replacementMethod;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
		if (memberInfo.getName().equals(name) && memberInfo.getDescriptor().equals(desc)) {
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
