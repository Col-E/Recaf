package me.coley.recaf.ui.control.dock;

import com.panemu.tiwulfx.control.dock.DetachableTabPane;

/**
 * Optimized implementation of {@link DetachableTabPane},
 * containing some method overrides for better performance.
 *
 * @author xDark
 */
public final class OptimizedDetachableTabPane extends DetachableTabPane {
	private static final String STYLESHEET_USER_AGENT = DetachableTabPane.class.getResource("tiwulfx-dock.css")
			.toExternalForm();

	@Override
	public String getUserAgentStylesheet() {
		// The base implementation re-streams the stylesheet per each instance
		return STYLESHEET_USER_AGENT;
	}
}
