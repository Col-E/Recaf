package me.coley.recaf.util.visitor;

import me.coley.recaf.RecafConstants;
import me.coley.recaf.util.Types;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * A visitor that strips illegal/malformed signature data from classes.
 *
 * @author Matt Coley
 */
public class IllegalSignatureRemovingVisitor extends ClassVisitor {
	private boolean detected;

	/**
	 * @param cv
	 * 		Parent visitor.
	 */
	public IllegalSignatureRemovingVisitor(ClassVisitor cv) {
		super(RecafConstants.ASM_VERSION, cv);
	}

	/**
	 * @return {@code true} if any illegal signatures were removed.
	 */
	public boolean hasDetectedIllegalSignatures() {
		return detected;
	}

	@Override
	public void visit(int version, int access, String name, String s, String superName, String[] interfaces) {
		super.visit(version, access, name, map(s, false), superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String s, Object value) {
		return super.visitField(access, name, desc, map(s, true), value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String s, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, map(s, false), exceptions);
		return new MethodVisitor(RecafConstants.ASM_VERSION, mv) {
			@Override
			public void visitLocalVariable(String name, String desc, String s, Label start, Label end, int index) {
				super.visitLocalVariable(name, desc, map(s, true), start, end, index);
			}
		};
	}

	private String map(String signature, boolean isOnClassOrMethod) {
		if (signature == null)
			return null;
		if (Types.isValidSignature(signature, isOnClassOrMethod))
			return signature;
		detected = true;
		return null;
	}
}
