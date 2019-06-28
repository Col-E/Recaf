package me.coley.recaf.util;

import me.coley.recaf.Recaf;
import org.objectweb.asm.Type;

import java.util.Locale;

import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.TypeUtil;

/**
 * Temporary location for {@link TypeUtil#filter(Type)}.
 * 
 * @author Matt
 *
 */
public class Misc {

	
	/**
	 * @return Runtime has JDK classes loaded.
	 */
	public static boolean canCompile() {
		try {
			javax.tools.JavaCompiler.class.toString();
			return true;
		} catch (Exception e) {
			Logging.error(e, false);
			return false;
		} catch (Throwable t) {
			return false;
		}
	}
	
	/**
	 * @return Runtime has JDK classes loaded.
	 */
	public static boolean canAttach() {
		try {
			com.sun.tools.attach.VirtualMachine.class.toString();
			return true;
		} catch (Exception e) {
			Logging.error(e, false);
			return false;
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Lower-cases the text, keeps first letter upper-case.
	 * 
	 * @param text
	 *            Text to change case of.
	 * @return Formatted text.
	 */
	public static String fixCase(String text) {
		return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
	}

	/**
	 * Get translation key by the enum name.
	 *
	 * @param group
	 *            translation group
	 * @param e
	 *            the enum
	 * @return translation key for the enum
	 */
	public static String getTranslationKey(String group, Enum<?> e) {
		return group + "." + e.name().toLowerCase(Locale.ENGLISH).replace("_", "");
	}

	/**
	 * @param clazz
	 *            Class to check.
	 * @return {@code true} if class represents a number.
	 */
	public static boolean isNumeric(Class<?> clazz) {
		//@formatter:off
		return Number.class.isAssignableFrom(clazz) || 
			clazz.equals(int.class)   || 
			clazz.equals(long.class)  ||
			clazz.equals(byte.class)  ||
			clazz.equals(short.class) ||
			clazz.equals(char.class)  ||
			clazz.equals(float.class) || 
			clazz.equals(double.class);
		//@formatter:on
	}

	/**
	 * Get type from descriptor, used only for FieldNode#desc.
	 * 
	 * @param desc
	 * @return
	 */
	public static Class<?> getTypeClass(String desc) {
		switch (desc) {
		case "B":
		case "C":
		case "I":
			return int.class;
		case "J":
			return long.class;
		case "F":
			return float.class;
		case "D":
			return double.class;
		case "Ljava/lang/String;":
			return String.class;
		default:
			return null;
		}
	}

	/**
	 * Trim the text to a last section, if needed.
	 * 
	 * @param item
	 *            Internal class name.
	 * @param delim
	 *            Delimter to trim to.
	 * @return Simple name.
	 */
	public static String trim(String item, String delim) {
		return item.indexOf(delim) > 0 ? item.substring(item.lastIndexOf(delim) + 1) : item;
	}

	/**
	 * @return {@code true} if the current context of the java process is for testing.
	 */
	public static boolean isTesting() {
		return System.getProperty("java.class.path").contains("junit");
	}

	/**
	 * @param key
	 * @return Should key be skipped if skip list contains prefix of key.
	 */
	public static boolean skipIgnoredPackage(String key) {
		if (Recaf.argsSerialized != null)
			for (String value : Recaf.argsSerialized.skipped)
				if (key.startsWith(value)) return true;
		return false;
	}
}
