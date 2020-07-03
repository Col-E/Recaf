package me.coley.recaf.util;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.ClassFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Utility to attempt basic recovery of classes that crash ASM.
 *
 * @author Matt
 */
public class IllegalBytecodePatcherUtil {
	/**
	 * @param value
	 * 		Raw bytecode of class that crashes ASM.
	 *
	 * @return Modified bytecode, hopefully that yields valid ASM parsable class.
	 * If any exception is thrown the original bytecode is returned.
	 */
	public static byte[] fix(byte[] value) {
		byte[] updated = value;
		try {
			updated = patchBinclub(updated);
			return updated;
		} catch (Throwable t) {
			// Fallback, yield original value
			Log.error(t, "Failed to patch class");
			return value;
		}
	}

	/**
	 * Patch binclub obfuscation.
	 *
	 * @param value
	 * 		Original class bytecode.
	 *
	 * @return ASM-parsable bytecode.
	 *
	 * @throws IOException
	 * 		Thrown when the class cannot be read from.
	 */
	private static byte[] patchBinclub(byte[] value) throws IOException {
		// Why are we padding this? Because illegal/incorrect offsets and other random length errors.
		byte[] copy = Arrays.copyOf(value, Math.min(value.length * 2, 65535));

		// TODO: Eventually due to the complexities that won't be supported by existing libraries a new minimal
		//  class file reader/writer system will need to be implemented...
		//  - Binclub patching works in about 50% of cases, but javassist still has EOF exceptions when reading.
		//
		/* Example of EOF that happens as a problem with the reading process (illegal code attr on field)
		java.io.EOFException: null
			at java.io.DataInputStream.readFully(DataInputStream.java:197)
			at java.io.DataInputStream.readFully(DataInputStream.java:169)
			at javassist.bytecode.CodeAttribute.<init>(CodeAttribute.java:109)
			at javassist.bytecode.AttributeInfo.read(AttributeInfo.java:87)
			at javassist.bytecode.FieldInfo.read(FieldInfo.java:278)
			at javassist.bytecode.FieldInfo.<init>(FieldInfo.java:72)
			at javassist.bytecode.ClassFile.read(ClassFile.java:812)
			at javassist.bytecode.ClassFile.<init>(ClassFile.java:185)
			at javassist.CtClassType.<init>(CtClassType.java:98)
			at javassist.ClassPool.makeClass(ClassPool.java:707)
		*/
		// Read the class file
		CtClass cl = ClassPool.getDefault().makeClass(new ByteArrayInputStream(copy));
		ClassFile cf = cl.getClassFile();
		// Remove illegal 0-length attributes.
		cf.getAttributes().removeIf(info -> info.get().length == 0);
		// Write class back to byte[]
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		cf.write(dos);
		// Validate
		return baos.toByteArray();
	}
}
