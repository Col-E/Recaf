package me.coley.recaf.mapping;

import me.coley.recaf.Recaf;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

/**
 * ClassRemapper extension that is crash-lenient. Bogus data that normally crashes ASM will be ignored.
 *
 * @author Matt
 */
public class LenientClassRemapper extends ClassRemapper {
	/**
	 * @param visitor
	 * 		Delegated class visitor.
	 * @param mapper
	 * 		Remapper of types.
	 */
	public LenientClassRemapper(ClassVisitor visitor, SimpleRecordingRemapper mapper) {
		super(visitor, mapper);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (isBogus(signature))
			signature = null;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
		if (isBogus(signature))
			signature = null;
		return super.visitRecordComponent(name, descriptor, signature);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if (isBogus(signature))
			signature = null;
		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor,
									 String signature, String[] exceptions) {
		if (isBogus(signature))
			signature = null;
		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}

	private static boolean isBogus(String signature) {
		try {
			new SignatureReader(signature).accept(new SignatureVisitor(Recaf.ASM_VERSION) {});
			return false;
		} catch (Exception ex) {
			return true;
		}
	}
}
