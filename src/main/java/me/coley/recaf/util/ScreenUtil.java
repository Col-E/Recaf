package me.coley.recaf.util;

import javafx.stage.Screen;

/**
 * Used to ensure that larger windows do not exceed the screen bounds.
 * 
 * @author Matt
 */
public class ScreenUtil {
	private final static Screen screen = Screen.getPrimary();

	/**
	 * @param screen
	 * @return Main window width.
	 */
	public static int prefWidth() {
		double w = screen.getBounds().getWidth();
		if (w > 1400) {
			return 1300;
		}
		if (w > 1000) {
			return 1200;
		}
		return (int) (w - 80);
	}

	/**
	 * @param screen
	 * @return Main window height.
	 */
	public static int prefHeight() {
		double h = screen.getBounds().getHeight();
		if (h > 900) {
			return 880;
		}
		if (h > 800) {
			return 800;
		}
		return (int) (h - 100);
	}
}
