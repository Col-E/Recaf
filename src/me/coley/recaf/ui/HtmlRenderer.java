package me.coley.recaf.ui;

import org.objectweb.asm.Type;

import me.coley.recaf.Options;

public interface HtmlRenderer {
	final String colBlueDark = "#193049";
	final String colTealDark = "#154234";
	final String colGreenDark = "#184216";
	final String colRedDark = "#351717";
	final String colGray = "#555555";

	/**
	 * HTML escape '&', '<' and '>'.
	 * 
	 * @param s
	 *            Text to escape
	 * @return
	 */
	default String escape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/**
	 * Converts a given type to a string.
	 * 
	 * @param type
	 * @return
	 */
	default String getTypeStr(Type type) {
		return getTypeStr(type, null);
	}

	/**
	 * Converts a given type to a string. Output will be simplified if enabled
	 * in passed options.
	 * 
	 * @param type
	 * @param options
	 * @return
	 */
	default String getTypeStr(Type type, Options options) {
		String s = "";
		if (type.getDescriptor().length() == 1) {
			switch (type.getDescriptor().charAt(0)) {
			case 'Z':
				return "boolean";
			case 'I':
				return "int";
			case 'J':
				return "long";
			case 'D':
				return "double";
			case 'F':
				return "float";
			case 'B':
				return "byte";
			case 'C':
				return "char";
			case 'S':
				return "short";
			case 'V':
				return "void";
			default:
				return type.getDescriptor();
			}
		} else {
			s += type.getInternalName();
		}
		if (options != null && options.opcodeSimplifyDescriptors && s.contains("/")) {
			s = s.substring(s.lastIndexOf("/") + 1);
			if (s.endsWith(";")) {
				s = s.substring(0, s.length() - 1);
			}
		}
		return s;
	}

	/**
	 * Italicize the given text.
	 * 
	 * @param input
	 * @return
	 */
	default String italic(String input) {
		return "<i>" + input + "</i>";
	}

	/**
	 * Color the given text.
	 * 
	 * @param color
	 * @param input
	 * @return
	 */
	default String color(String color, String input) {
		return "<span style=\"color:" + color + ";\">" + input + "</span>";
	}
}
