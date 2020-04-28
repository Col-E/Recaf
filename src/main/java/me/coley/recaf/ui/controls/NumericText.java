package me.coley.recaf.ui.controls;

import javafx.scene.control.TextField;

/**
 * TextField that with a numeric text parse.
 *
 * @author Matt
 */
public class NumericText extends TextField {
	/**
	 * @return Generic number, {@code null} if text does not represent any number format.
	 */
	public Number get() {
		String text = getText();
		if(text.matches("\\d+"))
			return Integer.parseInt(text);
		else if(text.matches("\\d+\\.?\\d*[dD]?")) {
			if(text.toLowerCase().contains("d"))
				return Double.parseDouble(text.substring(0, text.length() - 1));
			else
				return Double.parseDouble(text);
		} else if(text.matches("\\d+\\.?\\d*[fF]"))
			return Float.parseFloat(text.substring(0, text.length() - 1));
		else if(text.matches("\\d+\\.?\\d*[lL]"))
			return Long.parseLong(text.substring(0, text.length() - 1));
		return null;
	}
}
