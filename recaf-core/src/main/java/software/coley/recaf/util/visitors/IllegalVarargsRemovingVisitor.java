package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.util.AccessFlag;

import static org.objectweb.asm.Opcodes.ACC_VARARGS;

/**
 * A visitor that strips illegal varargs flags from methods.
 *
 * @author Matt Coley
 */
public class IllegalVarargsRemovingVisitor extends ClassVisitor {
	private boolean detected;

	/**
	 * @param cv
	 * 		Parent visitor.
	 */
	public IllegalVarargsRemovingVisitor(@Nullable ClassVisitor cv) {
		super(RecafConstants.getAsmVersion(), cv);
	}

	/**
	 * @return {@code true} if any illegal varargs were removed.
	 */
	public boolean hasDetectedIllegalVarargs() {
		return detected;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (AccessFlag.isVarargs(access)) {
			Type methodType = Type.getMethodType(descriptor);
			Type[] argumentTypes = methodType.getArgumentTypes();
			if (argumentTypes.length == 0 || argumentTypes[argumentTypes.length - 1].getSort() != Type.ARRAY) {
				access = AccessFlag.removeFlag(access, ACC_VARARGS);
				detected = true;
			}
		}
		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}
}
