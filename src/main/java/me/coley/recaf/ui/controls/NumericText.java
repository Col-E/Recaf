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
		// Normal int
		if(text.matches("-?\\d+"))
			return Integer.parseInt(text);
		// Double
		else if(text.matches("-?\\d+\\.?\\d*[dD]?")) {
			if(text.toLowerCase().contains("d"))
				return Double.parseDouble(text.substring(0, text.length() - 1));
			else
				return Double.parseDouble(text);
		}
		// Float
		else if(text.matches("-?\\d+\\.?\\d*[fF]"))
			return Float.parseFloat(text.substring(0, text.length() - 1));
		// Long
		else if(text.matches("-?\\d+\\.?\\d*[lL]"))
			return Long.parseLong(text.substring(0, text.length() - 1));
		// Hex int
		else if(text.matches("-?0x\\d+"))
			return (int) Long.parseLong(text.replace("0x", ""), 16);
		// Hex long
		else if(text.matches("-?0x\\d+[lL]"))
			return Long.parseLong(text.substring(0, text.length() - 1).replace("0x", ""), 16);
		return null;
	}
}
