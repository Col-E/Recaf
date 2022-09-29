package me.coley.recaf.util.visitor;

import me.coley.recaf.RecafConstants;
import me.coley.recaf.code.MemberInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Simple visitor for removing a matched {@link MemberInfo}.
 *
 * @author Matt Coley
 */
public class MemberRemovingVisitor extends ClassVisitor {
	private final MemberInfo memberInfo;
	private boolean removed;

	/**
	 * @param cv
	 * 		Parent visitor where the removal will be applied in.
	 * @param memberInfo
	 * 		Member to remove.
	 */
	public MemberRemovingVisitor(ClassVisitor cv, MemberInfo memberInfo) {
		super(RecafConstants.getAsmVersion(), cv);
		this.memberInfo = memberInfo;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
		if (memberInfo.isField() && memberInfo.getName().equals(name) && memberInfo.getDescriptor().equals(desc)) {
			removed = true;
			return null;
		}
		return super.visitField(access, name, desc, sig, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
		if (memberInfo.isMethod() && memberInfo.getName().equals(name) && memberInfo.getDescriptor().equals(desc)) {
			removed = true;
			return null;
		}
		return super.visitMethod(access, name, desc, sig, exceptions);
	}

	/**
	 * @return {@code true} when a field or method was removed.
	 */
	public boolean isRemoved() {
		return removed;
	}
}
