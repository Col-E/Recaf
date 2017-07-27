package me.coley.edit.ui.component;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

@SuppressWarnings("serial")
public class TabWrapper extends JPanel {
	private final JTabbedPane pane;
	private final Map<String, Component> children = new HashMap<>();
	private final Map<String, Integer> childrenIndecies = new HashMap<>();

	public TabWrapper() {
		setLayout(new BorderLayout());
		pane = new JTabbedPane(JTabbedPane.TOP);
		pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		add(pane, BorderLayout.CENTER);
	}

	/**
	 * Adds a tab to the panel.
	 * 
	 * @param title
	 * @param component
	 */
	public void addTab(String title, Component component) {
		pane.add(title, component);
		children.put(title, component);
		if (!shouldCache(title, component)) {
			childrenIndecies.put(title, getTabCount() - 1);
		}
	}

	/**
	 * Determines if the tab with the given title and component should cached
	 * for redirection, instead of duplicating tabs.
	 * 
	 * @param title
	 * @param component
	 * @return
	 */
	private boolean shouldCache(String title, Component component) {
		return title.contains("Error: ");
	}

	/**
	 * Get the number of open tabs.
	 * 
	 * @return
	 */
	public int getTabCount() {
		return pane.getTabCount();
	}

	/**
	 * Set the selected tab.
	 * 
	 * @param index
	 */
	public void setSelectedTab(int index) {
		pane.setSelectedIndex(index);
	}

	/**
	 * Check if a tab by the given title exists and is available for
	 * redirection.
	 * 
	 * @param title
	 * @return
	 */
	public boolean hasCached(String title) {
		return childrenIndecies.containsKey(title);
	}

	/**
	 * Retrieve the index of the cached tab by its title.
	 * 
	 * @param title
	 * @return
	 */
	public int getCachedIndex(String title) {
		return childrenIndecies.get(title);
	}
}
