package me.coley.recaf.ui.component.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import me.coley.recaf.ui.component.ReleaseListener;

/**
 * Wrapper for Tabbed Pane, providing extra abilities such as tab removal and
 * redirection.
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class TabbedPanel extends JPanel {
	/**
	 * Wrapped tabbed pane.
	 */
	private final JTabbedPane pane;
	private final Map<String, Component> children = new HashMap<>();
	private final Map<Component, String> childrenReverse = new HashMap<>();

	public TabbedPanel() {
		setLayout(new BorderLayout());
		pane = new JTabbedPane(JTabbedPane.TOP);
		pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		pane.addMouseListener(new ReleaseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
				// Only close tabs when middle-clicked
				if (e.getButton() != MouseEvent.BUTTON2) {
					return;
				}
				int index = pane.getSelectedIndex();
				if (index >= 0) {
					String key = childrenReverse.remove(pane.getSelectedComponent());
					children.remove(key);
					pane.remove(index);
				}
			}
		});
		add(pane, BorderLayout.CENTER);
	}

	/**
	 * Adds a tab to the panel.
	 *
	 * @param title The tab's title.
	 * @param component The component.
	 *
	 * TODO: What is the component for?
	 *
	 *	- Charles
	 */
	public void addTab(String title, Component component) {
		pane.add(title, component);
		if (!shouldCache(title, component)) {
			children.put(title, component);
			childrenReverse.put(component, title);
		}
	}

	/**
	 * Determines if the tab with the given title and component should
	 * cached for redirection, instead of duplicating tabs.
	 *
	 * @param title The tab's title.
	 * @param component The component.
	 *
	 * TODO: What is the component for?
	 *
	 *	- Charles
	 *
	 * @return true if the tab should be cached.
	 */
	private boolean shouldCache(String title, Component component) {
		return title.contains("Error: ");
	}

	/**
	 * @return The number of open tabs.

	 */
	public int getTabCount() {
		return pane.getTabCount();
	}

	/**
	 * Set the selected tab.
	 *
	 * @param index Index of the tab.
	 */
	public void setSelectedTab(int index) {
		pane.setSelectedIndex(index);
	}

	/**
	 * Check if a tab by the given title exists and is available for
	 * redirection.
	 *
	 * @param title Title of the tab to check.
	 * @return true if the tab is available for redirection.
	 *
	 * TODO: Is the return accurate? I'm not entirely sure what this does.
	 *
	 *	- Charles
	 */
	public boolean hasCached(String title) {
		return children.containsKey(title);
	}

	/**
	 * Retrieve the index of the cached tab by its title.
	 *
	 * @param title The title of the tab.
	 * @return The tab's index.
	 */
	public int getCachedIndex(String title) {
		for (int i = 0; i < getTabCount(); i++) {
			Component component = pane.getComponentAt(i);
			if (childrenReverse.get(component).equals(title)) {
				return i;
			}
		}
		return -1;
	}
}
