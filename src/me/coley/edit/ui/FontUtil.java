package me.coley.edit.ui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;

public class FontUtil {
	public static final Font monospace;

	static {
		String consolas = null;
		String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		for (String font : fonts) {
			if (font.toLowerCase().contains("consolas")) {
				consolas = font;
			}
		}
		if (consolas == null) {
			monospace = new Font(Font.MONOSPACED, Font.TRUETYPE_FONT, 12);
		} else {
			monospace = new Font(consolas, Font.TRUETYPE_FONT, 12);
		}
	}
}
