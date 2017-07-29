package me.coley.recaf.ui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class FontUtil {
	private static final AffineTransform affinetransform = new AffineTransform();     
	private static final  FontRenderContext frc = new FontRenderContext(affinetransform,true,true); 
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

	public static Rectangle2D getStringBounds(String text, Font font) {
		return font.getStringBounds(text, frc);
	}
}
