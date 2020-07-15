package me.coley.recaf.util;

import com.sun.tools.classfile.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility to attempt basic recovery of classes that crash ASM.
 *
 * @author Matt
 */
public class IllegalBytecodePatcherUtil {
	/**
	 * @param classes
	 * 		Successfully loaded classes in the input.
	 * @param invalidClasses
	 * 		The complete map of invalid classes in the input.
	 * @param value
	 * 		Raw bytecode of a class that crashes ASM.
	 *
	 * @return Modified bytecode, hopefully that yields valid ASM parsable class.
	 * If any exception is thrown the original bytecode is returned.
	 */
	public static byte[] fix(Map<String, byte[]> classes, Map<String, byte[]> invalidClasses, byte[] value) {
		try {
			byte[] updated = value;
			String str = new String(value).toLowerCase();
			// TODO: A good automated system to detect the problem would be handy
			//  - Something better than this obviously since these are essentially swappable watermarks
			if (str.contains("binscure") || str.contains("binclub") || str.contains("java/yeet")) {
				updated = patchBinclub(updated);
			} else {
				// TODO: Other obfuscators that create invalid classes, like Paramorphism, should be supported
				//  - Some code for this already exists in the discord group but its outdated...
				Log.info("Unknown protection on class file");
			}
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
		try {
			// Read into sun's class-file class
			ClassFile cf = ClassFile.read(new ByteArrayInputStream(value));
			// Remove illegal 0-length attributes
			List<Attributes> attributesList = new ArrayList<>();
			attributesList.add(cf.attributes);
			attributesList.addAll(Arrays.stream(cf.fields).map(f -> f.attributes).collect(Collectors.toSet()));
			attributesList.addAll(Arrays.stream(cf.methods).map(m -> m.attributes).collect(Collectors.toSet()));
			for (Attributes attributes : attributesList) {
				Attribute[] updatedAttrArray = attributes.attrs;
				for (int i = attributes.size() - 1; i >= 0; i--)
					if (attributes.get(i).attribute_length == 0 || attributes.get(i) instanceof DefaultAttribute)
						updatedAttrArray = remove(updatedAttrArray, i);
				setAttrs(attributes, updatedAttrArray);
			}
			// Swap illegal class names like "give up" to something legal
			for (ConstantPool.CPInfo info : cf.constant_pool.entries()) {
				if (info instanceof ConstantPool.CONSTANT_Class_info) {
					ConstantPool.CONSTANT_Class_info clsInfo = (ConstantPool.CONSTANT_Class_info) info;
					if (isIllegalName(clsInfo.getName())) {
						setInt(clsInfo, "name_index", cf.constant_pool.getClassInfo(cf.this_class).name_index);
					}
				}
			}
			// Write class-file back
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			new ClassWriter().write(cf, baos);
			// Attempt to pass-through asm to remove some extra junk,
			// which assists some decompilers in not totally shitting themselves.
			// The output is still trash, but at least there is output.
			if (ClassUtil.isValidClass(baos.toByteArray())) {
				try {
					ClassNode node = ClassUtil.getNode(new org.objectweb.asm.ClassReader(baos.toByteArray()),
							org.objectweb.asm.ClassReader.EXPAND_FRAMES);
					for (MethodNode mn : node.methods) {
						for (AbstractInsnNode insn : mn.instructions) {
							// These "ex-dee le may-may" named instructions are bogus.
							// They're in code-blocks that get skipped over and only serve to include junk
							// in the constant-pool that throws off decompilers.
							if (insn instanceof InvokeDynamicInsnNode) {
								InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
								if (indy.bsm.getOwner().equals("java/yeet") ||
										indy.name.equals("fuck") || indy.name.equals("yayeet")) {
									mn.instructions.set(insn, new InsnNode(Opcodes.NOP));
								}
							}
						}
					}
					return ClassUtil.toCode(node, 0);
				} catch (Throwable t) {
					Log.warn("Failed to remove junk INVOKEDYNAMIC instructions");
				}
			} else {
				Log.error("Binclub process failed to fully patch class. New ASM crash method?");
			}
			// Fallback if second-asm pass fails.
			// Classes should be valid at this point, but not entierly cleaned up.
			return baos.toByteArray();
		} catch (ConstantPoolException cpe) {
			throw new IOException(cpe);
		}
	}

	private static boolean isIllegalName(String name) {
		if (name.contains(" "))
			return true;
		return false;
	}

	@SuppressWarnings("all")
	private static <T> T remove(final T array, final int index) {
		int length = Array.getLength(array);
		if (length == 0)
			return array;
		Object result = Array.newInstance(array.getClass().getComponentType(), length - 1);
		System.arraycopy(array, 0, result, 0, index);
		if (index < length - 1)
			System.arraycopy(array, index + 1, result, index, length - index - 1);
		return (T) result;
	}

	private static void setAttrs(Attributes attributes, Attribute[] remove) {
		try {
			java.lang.reflect.Field f = Attributes.class.getDeclaredField("attrs");
			f.setAccessible(true);
			f.set(attributes, remove);
		} catch (Exception ex) {
			Log.error("Cannot update attributes? Did sun's CF format change?");
		}
	}

	private static void setInt(Object host, String name, int value) {
		try {
			java.lang.reflect.Field f = host.getClass().getDeclaredField(name);
			f.setAccessible(true);
			f.set(host, value);
		} catch (Exception ex) {
			Log.error("Cannot update int field: {}", ex.getMessage());
		}
	}
}
