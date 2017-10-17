package me.coley.recaf.ui;

import org.objectweb.asm.Type;

import me.coley.recaf.Recaf;
import me.coley.recaf.config.Colors;
import me.coley.recaf.config.Options;

/**
 * Utility methods creating HTML for labels.
 * 
 * @author Matt
 *
 */
public interface HtmlRenderer {
	/**
	 * HTML escape '&amp;', '&lt;' and '&gt;'.
	 *
	 * @param s
	 *            Text to escape
	 * @return Text with amp, lt, and gt escaped correctly.
	 */
	default String escape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/**
	 * Converts a given type to a string.
	 *
	 * @param type
	 *            The type object.
	 * @return String representation of the type object.
	 */
	default String getTypeStr(Type type) {
		return getTypeStr(type, null);
	}

	/**
	 * Converts a given type to a string. Output will be simplified if enabled
	 * in passed options.
	 * 
	 * @param type
	 *            The type object.
	 * @param options
	 *            Options object.
	 * @return String representation of the type object.
	 * @see me.coley.recaf.config.Options
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
	 *            Text to italicize.
	 * @return HTML markup to italicize the text.
	 */
	default String italic(String input) {
		return "<i>" + input + "</i>";
	}

	/**
	 * Bold the given text.
	 *
	 * @param input
	 *            Text to bold.
	 * @return HTML markup with the text bolded.
	 */
	default String bold(String input) {
		return "<b>" + input + "</b>";
	}

	/**
	 * Color the given text.
	 *
	 * @param color
	 *            The color to turn the text, must be a valid HTML color.
	 * @param input
	 *            The text to color.
	 * @return HTML markup with the text colored appropriately.
	 */
	default String color(String color, String input) {
		return "<font color=\"" + color + "\">" + input + "</font>";
	}

	/**
	 * @return Colors instance.
	 */
	default Colors getColors() {
		return Recaf.INSTANCE.colors;
	}
}
