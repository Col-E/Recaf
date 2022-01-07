package me.coley.recaf.util;

import me.coley.recaf.Recaf;
import me.coley.recaf.util.struct.Pair;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.ClassReader.*;

/**
 * Utilities for dealing with class-file loading/parsing.
 *
 * @author Matt
 */
public class ClassUtil {
	/**
	 * The offset from which a version and the version constant value is. For example, Java 8 is 52 <i>(44 + 8)</i>.
	 */
	public static final int VERSION_OFFSET = 44;

	/**
	 * @param name
	 * 		Internal class name.
	 *
	 * @return {@link org.objectweb.asm.ClassReader} loaded from runtime.
	 */
	public static ClassReader fromRuntime(String name) {
		try {
			return new ClassReader(name);
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
	 * 		Class reader to generate a node from.
	 * @param readFlags
	 * 		Flags to apply when generating the node.
	 *
	 * @return Node from reader.
	 */
	public static ClassNode getNode(ClassReader reader, int readFlags) {
		ClassNode node = new ClassNode();
		reader.accept(node, readFlags);
		return node;
	}

	/**
	 * @param node
	 * 		Node to convert back to bytecode.
	 * @param writeFlags
	 * 		Writer flags to use in conversion.
	 *
	 * @return Class bytecode.
	 */
	public static byte[] toCode(ClassNode node, int writeFlags) {
		ClassWriter cw = new ClassWriter(writeFlags);
		node.accept(cw);
		return cw.toByteArray();
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
		reader.accept(new ClassVisitor(Recaf.ASM_VERSION) {
			@Override
			public MethodVisitor visitMethod(int access, String vname, String vdesc, String
					signature, String[] exceptions) {
				if (name.equals(vname) && vdesc.equals(desc)) contains[0] = true;
				return null;
			}
		}, SKIP_DEBUG | SKIP_CODE);
		return contains[0];
	}

	/**
	 * @param reader
	 * 		Class to visit.
	 * @param name
	 * 		Name of field to check.
	 * @param desc
	 * 		Descriptor of field to check.
	 *
	 * @return {@code true} if the {@link org.objectweb.asm.ClassReader} contains the field by the
	 * given name &amp; descriptor.
	 */
	public static boolean containsField(ClassReader reader, String name, String desc) {
		boolean[] contains = {false};
		reader.accept(new ClassVisitor(Recaf.ASM_VERSION) {
			@Override
			public FieldVisitor visitField(int access, String vname, String vdesc, String
					signature, Object value) {
				if (name.equals(vname) && vdesc.equals(desc)) contains[0] = true;
				return null;
			}
		}, SKIP_DEBUG | SKIP_CODE);
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
		reader.accept(new ClassVisitor(Recaf.ASM_VERSION) {
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
	 *
	 * @return List of Pair&lt;method-name, method-descriptor&gt;
	 */
	public static List<Pair<String, String>> getMethodDefs(ClassReader reader) {
		List<Pair<String, String>> methods = new ArrayList<>();
		reader.accept(new ClassVisitor(Recaf.ASM_VERSION) {
			@Override
			public MethodVisitor visitMethod(int access, String vname, String vdesc, String
					signature, String[] exceptions) {
				methods.add(new Pair<>(vname, vdesc));
				return null;
			}
		}, SKIP_DEBUG | SKIP_CODE);
		return methods;
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
		reader.accept(new ClassVisitor(Recaf.ASM_VERSION) {
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

	/**
	 * @param reader
	 * 		Class to visit.
	 *
	 * @return List of Pair&lt;field-name, field-descriptor&gt;
	 */
	public static List<Pair<String, String>> getFieldDefs(ClassReader reader) {
		List<Pair<String, String>> fields = new ArrayList<>();
		reader.accept(new ClassVisitor(Recaf.ASM_VERSION) {
			@Override
			public FieldVisitor visitField(int access, String vname, String vdesc, String
					signature, Object value) {
				fields.add(new Pair<>(vname, vdesc));
				return null;
			}
		}, SKIP_DEBUG | SKIP_CODE);
		return fields;
	}

	/**
	 * Remove a field from the class.
	 *
	 * @param reader
	 * 		Reader containing the class.
	 * @param name
	 * 		Name of field to remove.
	 * @param desc
	 * 		Descriptor of field to remove.
	 *
	 * @return Updated bytecode of class.
	 */
	public static byte[] removeField(ClassReader reader, String name, String desc) {
		ClassWriter cw = new ClassWriter(0);
		reader.accept(new ClassVisitor(Recaf.ASM_VERSION, cw) {
			@Override
			public FieldVisitor visitField(int access, String vname, String vdesc, String
					signature, Object value) {
				// Skip given field, effectively removing it
				if (vname.endsWith(name) && vdesc.endsWith(desc))
					return null;
				return super.visitField(access, vname, vdesc, signature, value);
			}
		}, EXPAND_FRAMES);
		return cw.toByteArray();
	}

	/**
	 * Remove a method from the class.
	 *
	 * @param reader
	 * 		Reader containing the class.
	 * @param name
	 * 		Name of method to remove.
	 * @param desc
	 * 		Descriptor of method to remove.
	 *
	 * @return Updated bytecode of class.
	 */
	public static byte[] removeMethod(ClassReader reader, String name, String desc) {
		ClassWriter cw = new ClassWriter(0);
		reader.accept(new ClassVisitor(Recaf.ASM_VERSION, cw) {
			@Override
			public MethodVisitor visitMethod(int access, String vname, String vdesc, String
					signature, String[] exceptions) {
				// Skip given method, effectively removing it
				if (vname.endsWith(name) && vdesc.endsWith(desc))
					return null;
				return super.visitMethod(access, vname, vdesc, signature, exceptions);
			}
		}, EXPAND_FRAMES);
		return cw.toByteArray();
	}

	/**
	 * @param code
	 * 		Class bytecode.
	 *
	 * @return Class access. If an parse error occurred then return is {@code 0}.
	 */
	public static int getAccess(byte[] code) {
		try {
			return new ClassReader(code).getAccess();
		} catch(Exception ex) { /* Bad class file? */ return 0;}
	}

	/**
	 * @param code
	 * 		Class bytecode.
	 *
	 * @return Class major version. If an parse error occurred then return is {@link Opcodes#V1_8}.
	 */
	public static int getVersion(byte[] code) {
		try {
			return (((code[6] & 0xFF) << 8) | (code[7] & 0xFF));
		} catch(Exception ex) { /* Bad class file? */ return Opcodes.V1_8;}
	}

	/**
	 * @param data
	 * 		Potential class bytecode.
	 *
	 * @return {@code true} if data has class magic prefix.
	 */
	public static boolean isClass(byte[] data) {
		return data.length >= 4 &&
				0xCAFEBABEL == ((
						(0xFF & data[0]) << 24L |
						(0xFF & data[1]) << 16L |
						(0xFF & data[2]) << 8L  |
						 0xFF & data[3]) & 0xFFFFFFFFL);
	}

	/**
	 * Copies method metadata.
	 *
	 * @param from method to copy from.
	 * @param to method to copy to.
	 */
	public static void copyMethodMetadata(MethodNode from, MethodNode to) {
		if (to.invisibleAnnotations == null && from.invisibleAnnotations != null)
			to.invisibleAnnotations = new ArrayList<>();
		if (to.visibleAnnotations == null && from.visibleAnnotations != null)
			to.visibleAnnotations = new ArrayList<>();
		if (to.invisibleTypeAnnotations == null && from.invisibleTypeAnnotations != null)
			to.invisibleTypeAnnotations = new ArrayList<>();
		if (to.visibleTypeAnnotations == null && from.visibleTypeAnnotations != null)
			to.visibleTypeAnnotations = new ArrayList<>();
		if (to.invisibleLocalVariableAnnotations == null && from.invisibleLocalVariableAnnotations != null)
			to.invisibleLocalVariableAnnotations = new ArrayList<>();
		if (to.visibleLocalVariableAnnotations == null && from.visibleLocalVariableAnnotations != null)
			to.visibleLocalVariableAnnotations = new ArrayList<>();
		updateAnnotationList(to.invisibleAnnotations, from.invisibleAnnotations);
		updateAnnotationList(to.visibleAnnotations, from.visibleAnnotations);
		updateAnnotationList(to.invisibleTypeAnnotations, from.invisibleTypeAnnotations);
		updateAnnotationList(to.visibleTypeAnnotations, from.visibleTypeAnnotations);
		updateAnnotationList(to.invisibleLocalVariableAnnotations, from.invisibleLocalVariableAnnotations);
		updateAnnotationList(to.visibleLocalVariableAnnotations, from.visibleLocalVariableAnnotations);
		to.invisibleParameterAnnotations = from.invisibleParameterAnnotations;
		to.visibleParameterAnnotations = from.visibleParameterAnnotations;
		to.visibleAnnotableParameterCount = from.visibleAnnotableParameterCount;
		to.invisibleAnnotableParameterCount = from.invisibleAnnotableParameterCount;
	}

	@SuppressWarnings("all")
	private static <T extends AnnotationNode> void updateAnnotationList(List<T> to, List<T> from) {
		// No data to copy
		if (from == null)
			return;
		// Add if not null
		if (to != null)
			for (T node : from) {
				String fromType = node.desc;
				// We are replacing the annotation of the matching type.
				// You can only have one of any single type on an item.
				if (node instanceof TypeAnnotationNode) {
					// For type annotations we need to do some extra checks...
					to.removeIf(n -> {
						if (!n.desc.equals(fromType)) {
							return false;
						}
						if (n instanceof TypeAnnotationNode) {
							TypeAnnotationNode fromNode = (TypeAnnotationNode) node;
							TypeAnnotationNode toNode = (TypeAnnotationNode) n;
							// Type paths must match as well, indicating the target is the same
							return fromNode.typePath.toString().equals(toNode.typePath.toString());
						}
						return false;
					});
				} else {
					to.removeIf(n -> n.desc.equals(fromType));
				}

				to.add(node);
			}

	}

	/**
	 * Copies field metadata.
	 *
	 * @param from field to copy from.
	 * @param to field to copy to.
	 */
	public static void copyFieldMetadata(FieldNode from, FieldNode to) {
		to.invisibleAnnotations = from.invisibleAnnotations;
		to.visibleAnnotations = from.visibleAnnotations;
		to.invisibleTypeAnnotations = from.invisibleTypeAnnotations;
		to.visibleTypeAnnotations = from.visibleTypeAnnotations;
	}

	/**
	 * Strip debug information from the given class bytecode.
	 *
	 * @param code
	 * 		Class bytecode.
	 *
	 * @return Class bytecode, modified to remove all debug information.
	 */
	public static byte[] stripDebugForDecompile(byte[] code) {
		if (code == null || code.length <= 10)
			return code;
		ClassReader cr = new ClassReader(code);
		ClassWriter cw = new ClassWriter(0);
		cr.accept(cw, SKIP_DEBUG | EXPAND_FRAMES);
		return cw.toByteArray();
	}

	/**
	 * Validate the class can be parsed by ASM.
	 *
	 * @param value
	 * 		Class bytecode.
	 *
	 * @return {@code true} when the class can be read by ASM.
	 */
	public static boolean isValidClass(byte[] value) {
		if (!isClass(value))
			return false;
		try {
			getNode(new ClassReader(value), SKIP_FRAMES);
			return true;
		} catch(Throwable t) {
			return false;
		}
	}

}
