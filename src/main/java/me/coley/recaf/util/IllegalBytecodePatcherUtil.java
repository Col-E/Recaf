package me.coley.recaf.util;

import com.sun.tools.classfile.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
		byte[] updated = value;
		try {
			// TODO: Auto-detecting the problem so transformations that don't need to be done are not called
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
				for (int i = attributes.attrs.length - 1; i >= 0; i--)
					if (attributes.attrs[i].attribute_length == 0)
						updatedAttrArray = remove(updatedAttrArray, i);
				setAttrs(attributes, updatedAttrArray);
			}
			// Write class-file back
			String name = cf.getName();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			new ClassWriter().write(cf, baos);
			return baos.toByteArray();
		} catch (ConstantPoolException cpe) {
			throw new IOException(cpe);
		}
	}

	@SuppressWarnings("all")
	private static <T> T remove(final T array, final int index) {
		int length = Array.getLength(array);
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
}
