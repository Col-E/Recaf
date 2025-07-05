package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import software.coley.recaf.RecafConstants;

import java.util.Arrays;
import java.util.Objects;

/**
 * A visitor that strips long named exceptions from methods.
 * <p/>
 * Not compatible with {@link ClassWriter#ClassWriter(ClassReader, int)} since exceptions get copied when you provide
 * an input reader to copy data from.
 *
 * @author Matt Coley
 */
public class LongExceptionRemovingVisitor extends ClassVisitor {
	private final int maxAllowedLength;
	private boolean detected;

	/**
	 * @param cv
	 * 		Parent visitor.
	 * @param maxAllowedLength
	 * 		Max length of allowed exception types.
	 */
	public LongExceptionRemovingVisitor(@Nullable ClassVisitor cv, int maxAllowedLength) {
		super(RecafConstants.getAsmVersion(), cv);
		this.maxAllowedLength = maxAllowedLength;
	}

	/**
	 * @return {@code true} if any long exceptions were removed.
	 */
	public boolean hasDetectedLongExceptions() {
		return detected;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (exceptions != null) {
			boolean removed = false;
			for (int i = 0; i < exceptions.length; i++) {
				if (exceptions[i].length() > maxAllowedLength) {
					exceptions[i] = null;
					removed = true;
				}
			}
			if (removed) {
				exceptions = Arrays.stream(exceptions)
						.filter(Objects::nonNull)
						.toArray(String[]::new);
				if (exceptions.length == 0)
					exceptions = null;
				detected = true;
			}
		}

		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}
}
