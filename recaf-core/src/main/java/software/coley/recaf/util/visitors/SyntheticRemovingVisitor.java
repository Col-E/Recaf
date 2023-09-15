package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.RecafConstants;

/**
 * A visitor that strips synthetic flags from classes.
 *
 * @author Matt Coley
 */
public class SyntheticRemovingVisitor extends ClassVisitor {
	/**
	 * @param cv
	 * 		Parent visitor.
	 */
	public SyntheticRemovingVisitor(@Nullable ClassVisitor cv) {
		super(RecafConstants.getAsmVersion(), cv);
	}

	private static int strip(int access) {
		return access & ~(Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE);
	}

	@Override
	public void visit(int version, int access, String name, String sig, String superName, String[] interfaces) {
		access = strip(access);
		super.visit(version, access, name, sig, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
		access = strip(access);
		return super.visitField(access, name, desc, sig, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
		access = strip(access);
		return super.visitMethod(access, name, desc, sig, exceptions);
	}
}
