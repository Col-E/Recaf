package me.coley.recaf.util;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Utilities for dealing with class-file loading/parsing.
 *
 * @author Matt
 */
public class ClassUtil {
	/**
	 * @param name
	 * 		Internal class name.
	 *
	 * @return {@link org.objectweb.asm.ClassReader} loaded from runtime.
	 */
	public static ClassReader fromRuntime(String name) {
		try {
			URL url = ClassLoader.getSystemResource(name.replace('.', '/') + ".class");
			if (url == null) {
				return null;
			}
			try (InputStream in = url.openStream()) {
				return new ClassReader(in);
			}
		} catch(IOException e) {
			// Expected / allowed: ignore these
		} catch(Exception ex) {
			// Unexpected
			throw new IllegalStateException("Failed to load class from runtime: " + name, ex);
		}
		return null;
	}

	/**
	 * @param reader
	 * 		Class to visit.
	 * @param name
	 * 		Name of method to check.
	 * @param desc
	 * 		Descriptor of method to check.
	 *
	 * @return {@code true} if the {@link org.objectweb.asm.ClassReader} contains the method by the
	 * given name &amp; descriptor.
	 */
	public static boolean containsMethod(ClassReader reader, String name, String desc) {
		boolean[] contains = {false};
		reader.accept(new ClassVisitor(Opcodes.ASM7) {
			@Override
			public MethodVisitor visitMethod(int access, String vname, String vdesc, String
					signature, String[] exceptions) {
				if (name.equals(vname) && vdesc.equals(desc)) contains[0] = true;
				return null;
			}
		}, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
		return contains[0];
	}

	/**
	 * @param reader
	 * 		Class to visit.
	 * @param readFlags
	 * 		ClassReader flags to apply.
	 * @param name
	 * 		Name of method to fetch.
	 * @param desc
	 * 		Descriptor of method to fetch.
	 *
	 * @return {@link org.objectweb.asm.tree.MethodNode Method} matching the given definition in
	 * the given class.
	 */
	public static MethodNode getMethod(ClassReader reader, int readFlags, String name, String desc) {
		MethodNode[] method = {null};
		reader.accept(new ClassVisitor(Opcodes.ASM7) {
			@Override
			public MethodVisitor visitMethod(int access, String vname, String vdesc, String
					signature, String[] exceptions) {
				if(name.equals(vname) && vdesc.equals(desc)) {
					MethodNode vmethod = new MethodNode(access, vname, vdesc, signature, exceptions);
					method[0] = vmethod;
					return vmethod;
				}
				return null;
			}
		}, readFlags);
		return method[0];
	}

	/**
	 * @param reader
	 * 		Class to visit.
	 * @param readFlags
	 * 		ClassReader flags to apply.
	 * @param name
	 * 		Name of field to fetch.
	 * @param desc
	 * 		Descriptor of field to fetch.
	 *
	 * @return {@link org.objectweb.asm.tree.FieldNode Field} matching the given definition in
	 * the given class.
	 */
	public static FieldNode getField(ClassReader reader, int readFlags, String name, String desc) {
		FieldNode[] field = {null};
		reader.accept(new ClassVisitor(Opcodes.ASM7) {
			@Override
			public FieldVisitor visitField(int access, String vname, String vdesc, String signature, Object value) {
				if(name.equals(vname) && vdesc.equals(desc)) {
					FieldNode vfield = new FieldNode(access, vname, vdesc, signature, value);
					field[0] = vfield;
					return vfield;
				}
				return null;
			}
		}, readFlags);
		return field[0];
	}
}
