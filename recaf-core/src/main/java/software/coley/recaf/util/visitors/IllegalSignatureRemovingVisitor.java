package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;
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
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, map(signature, Types.SignatureContext.CLASS), superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		return super.visitField(access, name, desc, map(signature, Types.SignatureContext.FIELD), value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, map(signature, Types.SignatureContext.METHOD), exceptions);
		return new MethodVisitor(RecafConstants.getAsmVersion(), mv) {
			@Override
			public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
				super.visitLocalVariable(name, desc, map(signature, Types.SignatureContext.FIELD), start, end, index);
			}
		};
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
		return super.visitRecordComponent(name, descriptor, map(signature, Types.SignatureContext.METHOD));
	}

	@Override
	public void visitPermittedSubclass(String permittedSubclass) {
		// While not a signature, its metadata not used at runtime that can confuse RE tools.
		if (!Types.isValidDesc(permittedSubclass)) {
			detected = true;
			return;
		}
		super.visitPermittedSubclass(permittedSubclass);
	}

	@Nullable
	private String map(@Nullable String signature, @Nonnull Types.SignatureContext type) {
		if (Types.isValidSignature(signature, type))
			return signature;
		detected = true;
		return null;
	}
}
