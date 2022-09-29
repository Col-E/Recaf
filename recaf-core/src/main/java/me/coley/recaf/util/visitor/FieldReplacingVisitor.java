package me.coley.recaf.util.visitor;

import me.coley.recaf.RecafConstants;
import me.coley.recaf.code.MemberInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.tree.FieldNode;

/**
 * Simple visitor for replacing a matched {@link MemberInfo}.
 *
 * @author Matt Coley
 */
public class FieldReplacingVisitor extends ClassVisitor {
	private final MemberInfo memberInfo;
	private final FieldNode replacementField;
	private boolean replaced;

	/**
	 * @param cv
	 * 		Parent visitor where the removal will be applied in.
	 * @param memberInfo
	 * 		Details of the field to replace.
	 * @param replacementField
	 * 		Field to replace with.
	 */
	public FieldReplacingVisitor(ClassVisitor cv, MemberInfo memberInfo, FieldNode replacementField) {
		super(RecafConstants.getAsmVersion(), cv);
		this.memberInfo = memberInfo;
		this.replacementField = replacementField;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
		if (memberInfo.getName().equals(name) && memberInfo.getDescriptor().equals(desc)) {
			replaced = true;
			replacementField.accept(cv);
			return null;
		}
		return super.visitField(access, name, desc, sig, value);
	}

	/**
	 * @return {@code true} when the field was replaced.
	 */
	public boolean isReplaced() {
		return replaced;
	}
}
