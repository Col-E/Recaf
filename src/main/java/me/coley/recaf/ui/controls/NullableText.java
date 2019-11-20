package me.coley.recaf.ui.controls;

import javafx.scene.control.TextField;

/**
 * TextField with {@code null} text for empty strings.
 *
 * @author Matt
 */
public class NullableText extends TextField {
	/**
	 * @return Wrapper of standard 'getText' but empty strings are returned as {@code null}.
	 */
	public String get() {
		String text = super.getText();
		if(text == null || text.trim().isEmpty())
			return null;
		return text;
	}
}
