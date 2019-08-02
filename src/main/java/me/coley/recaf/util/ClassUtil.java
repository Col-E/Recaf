package me.coley.recaf.util;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
			Class<?> loaded = ClasspathUtil.getSystemClass(normalize(name));
			return new ClassReader(loaded.getName());
		} catch(ClassNotFoundException | IOException e) {
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
		AtomicBoolean contains = new AtomicBoolean();
		reader.accept(new ClassVisitor(Opcodes.ASM7) {
			@Override
			public MethodVisitor visitMethod(int access, String vname, String vdesc, String
					signature, String[] exceptions) {
				if (name.equals(vname) && vdesc.equals(desc))
					contains.set(true);
				return null;
			}
		}, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
		return contains.get();
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
		AtomicReference<MethodNode> method = new AtomicReference<>();
		reader.accept(new ClassVisitor(Opcodes.ASM7) {
			@Override
			public MethodVisitor visitMethod(int access, String vname, String vdesc, String
					signature, String[] exceptions) {
				if(name.equals(vname) && vdesc.equals(desc)) {
					MethodNode vmethod = new MethodNode(access, vname, vdesc, signature, exceptions);
					method.set(vmethod);
					return vmethod;
				}
				return null;
			}
		}, readFlags);
		return method.get();
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
		AtomicReference<FieldNode> field = new AtomicReference<>();
		reader.accept(new ClassVisitor(Opcodes.ASM7) {
			@Override
			public FieldVisitor visitField(int access, String vname, String vdesc, String signature, Object value) {
				if(name.equals(vname) && vdesc.equals(desc)) {
					FieldNode vfield = new FieldNode(access, vname, vdesc, signature, value);
					field.set(vfield);
					return vfield;
				}
				return null;
			}
		}, readFlags);
		return field.get();
	}

	/**
	 * @param internal
	 * 		Internal class name.
	 *
	 * @return Standard class name.
	 */
	private static String normalize(String internal) {
		return internal.replace("/", ".").replace("$", ".");
	}
}
