package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.util.Types;

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
	public IllegalSignatureRemovingVisitor(@Nullable ClassVisitor cv) {
		super(RecafConstants.getAsmVersion(), cv);
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
		return new MethodVisitor(RecafConstants.getAsmVersion(), mv) {
			@Override
			public void visitLocalVariable(String name, String desc, String s, Label start, Label end, int index) {
				super.visitLocalVariable(name, desc, map(s, true), start, end, index);
			}
		};
	}

	private String map(String signature, boolean isTypeSignature) {
		if (signature == null)
			return null;
		if (Types.isValidSignature(signature, isTypeSignature))
			return signature;
		detected = true;
		return null;
	}
}
