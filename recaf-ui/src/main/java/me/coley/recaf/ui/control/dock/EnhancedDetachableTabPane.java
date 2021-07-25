package me.coley.recaf.ui.control.dock;

import com.panemu.tiwulfx.control.dock.DetachableTabPane;

/**
 * Enhanced implementation of {@link DetachableTabPane},
 * containing some method overrides for better performance.
 *
 * @author xDark
 */
public final class EnhancedDetachableTabPane extends DetachableTabPane {

    private static final String STYLESHEET_USER_AGENT = DetachableTabPane.class.getResource("tiwulfx-dock.css")
            .toExternalForm();

    @Override
    public String getUserAgentStylesheet() {
        return STYLESHEET_USER_AGENT;
    }
}
