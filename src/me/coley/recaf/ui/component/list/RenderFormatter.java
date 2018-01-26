package me.coley.recaf.ui.component.list;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.objectweb.asm.Type;

import me.coley.recaf.Recaf;
import me.coley.recaf.config.impl.ConfTheme;
import me.coley.recaf.config.impl.ConfUI;
import me.coley.recaf.ui.Fonts;

/**
 * @author Matt
 *
 * @param <T>
 */
public interface RenderFormatter<T> extends ListCellRenderer<T> {

	/**
	 * Sets the desired look of the list.
	 * 
	 * @param list
	 *            List to apply style to.
	 */
	default void formatList(JList<?> list) {
		list.setBackground(Color.decode(getTheme().listBackground));
	}

	/**
	 * Sets the desired look of the lavel.
	 * 
	 * @param label
	 *            Label to apply style to.
	 * @param selected
	 *            If label is selected or not.
	 */
	default void formatLabel(JLabel label, boolean selected) {
		label.setFont(Fonts.monospace);
		label.setOpaque(true);
		label.setBorder(BorderFactory.createEtchedBorder());
		if (selected) {
			label.setBackground(Color.decode(getTheme().listItemSelected));
		} else {
			label.setBackground(Color.decode(getTheme().listItemBackground));
		}
	}

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
	 * @see me.coley.recaf.config.UiConfig
	 */
	default String getTypeStr(Type type, ConfUI options) {
		String s = type.getDescriptor();
		// Check if field type. If so, then format as class name.
		if (!s.contains("(") && (s.length() == 1 || s.startsWith("L") || s.startsWith("["))) {
			s = type.getClassName();
		}
		if(s == null)
		{
			System.out.println("Type " + type + " has no descriptor or class");
			s = "" + type;
		}
		// If simplification is on, substring away package.
		if (options != null && options.opcodeSimplifyDescriptors && s.contains(".")) {
			s = s.substring(s.lastIndexOf(".") + 1);
		}
		// Return name in internal style
		return s.replace(".", "/");
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
	default ConfTheme getTheme() {
		return Recaf.INSTANCE.configs.theme;
	}
}
