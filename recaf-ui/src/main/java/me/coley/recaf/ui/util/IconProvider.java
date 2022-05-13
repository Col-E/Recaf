package me.coley.recaf.ui.util;

import javafx.scene.Node;

/**
 * Provides an icon.
 *
 * @author xDark
 */
@FunctionalInterface
public interface IconProvider {

	/**
	 * @return Provided icon.
	 */
	Node makeIcon();
}
