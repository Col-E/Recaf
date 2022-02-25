package me.coley.recaf.util;

import me.coley.cafedude.classfile.ClassFile;
import me.coley.cafedude.io.ClassFileReader;
import me.coley.cafedude.io.ClassFileWriter;

import java.util.Map;
import me.coley.cafedude.transform.IllegalStrippingTransformer;

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
			ClassFile cf = new ClassFileReader().read(value);

			new IllegalStrippingTransformer(cf).transform();

			// Patch oak classes (pre-java)
			//  - CafeDude does this by default
			if (cf.getVersionMajor() < 45 ||(cf.getVersionMajor() == 45 && cf.getVersionMinor() <= 2)) {
				// Bump version into range where class file format uses full length values
				cf.setVersionMajor(45);
				cf.setVersionMinor(3);
				return new ClassFileWriter().write(cf);
			}
			/*
			// TODO: A good automated system to detect the problem would be handy
			//  - Something better than this obviously since these are essentially swappable watermarks
			String str = new String(value).toLowerCase();
			else if (str.contains("binscure") || str.contains("binclub") || str.contains("java/yeet")) {
				return patchBinscure(cf);
			} else {
				// TODO: Other obfuscators that create invalid classes, like Paramorphism, should be supported
				//  - Some code for this already exists in the discord group but its outdated...
				Log.info("Unknown protection on class file");
			}*/
			return new ClassFileWriter().write(cf);
		} catch (Throwable t) {
			// Fallback, yield original value
			Log.error(t, "Failed to patch class");
			return value;
		}
	}

	private static boolean isIllegalName(String name) {
		return name.matches(".*\\s+.*");
	}
}
