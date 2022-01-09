package me.coley.recaf.ui.pane;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.panemu.tiwulfx.control.dock.DetachableTabPane;
import com.panemu.tiwulfx.control.dock.DetachableTabPaneFactory;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.KeybindConfig;
import me.coley.recaf.ui.ClassView;
import me.coley.recaf.ui.ClassViewMode;
import me.coley.recaf.ui.FileView;
import me.coley.recaf.ui.FileViewMode;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.behavior.FileRepresentation;
import me.coley.recaf.ui.control.menu.ActionMenuItem;
import me.coley.recaf.ui.util.Animations;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.util.Menus;
import me.coley.recaf.ui.window.WindowBase;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Docking pane manager that handles creation of new {@link DetachableTabPane} instances.
 * Provides wrapper utility for adding new tabs to the most recently updated {@link DetachableTabPane}.
 *
 * @author Matt Coley
 */
public class DockingRootPane extends BorderPane {
	private static final Logger logger = Logging.get(DockingRootPane.class);
	private final DockingTabPaneFactory tabPaneFactory = new DockingTabPaneFactory();
	private final ListMultimap<String, KeyedTab> titleToTab = MultimapBuilder.treeKeys().arrayListValues().build();
	private final Stack<DetachableTabPane> recentTabPanes = new Stack<>();
	private SplitPane root = new SplitPane();
	private boolean isPendingSplit;

	/**
	 * Create new pane.
	 */
	public DockingRootPane() {
		setCenter(root);
	}

	/**
	 * Create a new split, the next added tab will be created in the new split.
	 *
	 * @param orientation
	 * 		Orientation of the split.
	 * @param dividerPositions
	 * 		Divider positions of items added to the new split.
	 *
	 * @return New root {@link SplitPane}.
	 */
	public SplitPane createNewSplit(Orientation orientation, double... dividerPositions) {
		// TODO: The initial space of the initial SplitPane never gets closed (drag workspace nav into adjacent tabpane)
		//   - But if we re-create the layout with dragging tabs into place the space is freeable...
		//   - Whats wrong with this setup?
		// Remove root
		setCenter(null);
		SplitPane oldRoot = root;
		// Create new split
		root = new SplitPane();
		root.setOrientation(orientation);
		root.setDividerPositions(dividerPositions);
		root.getItems().add(oldRoot);
		setCenter(root);
		// Invalidate recent tab panes.
		isPendingSplit = true;
		return root;
	}

	/**
	 * @return New detachable tab pane, tied into the current docking system.
	 */
	public DetachableTabPane createNewTabPane() {
		DetachableTabPane tabPane = new DetachableTabPane();
		tabPane.setDetachableTabPaneFactory(tabPaneFactory);
		tabPaneFactory.init(tabPane);
		return tabPane;
	}

	/**
	 * @param tabPane
	 * 		Tab pane to remove from the recent pane history.
	 */
	public void removeFromHistory(DetachableTabPane tabPane) {
		recentTabPanes.remove(tabPane);
	}

	private void add(Tab tab) {
		if (isPendingSplit || peekRecentTabPane() == null) {
			isPendingSplit = false;
			DetachableTabPane tabPane = createNewTabPane();
			tabPane.getTabs().add(tab);
			tabPane.getSelectionModel().select(tab);
			root.getItems().add(tabPane);
			pushRecentTabPane(tabPane);
		} else {
			DetachableTabPane recent = peekRecentTabPane();
			recent.getTabs().add(tab);
			recent.getSelectionModel().select(tab);
		}
	}

	/**
	 * Select an existing tab with the given title, or create a new one if it does not exist.
	 *
	 * @param info
	 * 		Item to pull name from as the title.
	 * @param contentFallback
	 * 		Tab content provider if the tab needs to be created.
	 *
	 * @return Opened tab.
	 */
	public Tab openInfoTab(ItemInfo info, Supplier<Node> contentFallback) {
		String key = info.getName();
		String title = StringUtil.shortenPath(info.getName());
		Tab tab = openTab(key, title, contentFallback);
		decorateTab(tab, info);
		return tab;
	}

	/**
	 * @param info
	 * 		Item to pull name from as the title.
	 *
	 * @return All tabs with matching name.
	 */
	public List<Tab> findInfoTabs(ItemInfo info) {
		List<Tab> tabs = new ArrayList<>();
		// This map holds all known open tabs, even if multiple share the same key name.
		titleToTab.values().forEach(tab -> {
			Node content = tab.getContent();
			if (info instanceof CommonClassInfo) {
				if (content instanceof ClassRepresentation) {
					ClassRepresentation representation = (ClassRepresentation) content;
					if (info.getName().equals(representation.getCurrentClassInfo().getName())) {
						tabs.add(tab);
					}
				}
			} else if (info instanceof FileInfo) {
				if (content instanceof FileRepresentation) {
					FileRepresentation representation = (FileRepresentation) content;
					if (info.getName().equals(representation.getCurrentFileInfo().getName())) {
						tabs.add(tab);
					}
				}
			}
		});
		return tabs;
	}

	/**
	 * @param info
	 * 		Item to pull name from as the title.
	 *
	 * @return First tab with matching name.
	 *
	 * @see #findInfoTabs(ItemInfo) for all matching tabs.
	 */
	public Tab findInfoTab(ItemInfo info) {
		List<Tab> tabs = findInfoTabs(info);
		if (tabs.isEmpty())
			return null;
		return tabs.get(0);
	}

	/**
	 * Select an existing tab with the given title, or create a new one if it does not exist.
	 *
	 * @param title
	 * 		Tab title.
	 * @param contentFallback
	 * 		Tab content provider if the tab needs to be created.
	 *
	 * @return Opened tab.
	 */
	public Tab openTab(String title, Supplier<Node> contentFallback) {
		return openTab(title, title, contentFallback);
	}

	/**
	 * @param key
	 * 		Tab lookup key.
	 * @param title
	 * 		Tab title.
	 * @param contentFallback
	 * 		Tab content provider if the tab needs to be created.
	 *
	 * @return Opened tab. New tab if {@code key} had no mapping, or existing if it did.
	 */
	private Tab openTab(String key, String title, Supplier<Node> contentFallback) {
		// Select if existing tab with title exists
		List<KeyedTab> tabs = titleToTab.get(key);
		if (tabs != null && !tabs.isEmpty()) {
			Tab target = tabs.get(tabs.size() - 1);
			TabPane parent = target.getTabPane();
			parent.getSelectionModel().select(target);
			// Show little flash to bring attention to the open item
			if (Configs.display().flashOpentabs)
				Animations.animateNotice(target.getContent(), 1000);
			return target;
		} else {
			// Create new tab if it does not exist
			return createTab(key, title, contentFallback.get());
		}
	}

	/**
	 * Creates a new tab.
	 *
	 * @param title
	 * 		Tab title.
	 * @param content
	 * 		Tab content.
	 *
	 * @return Created tab.
	 */
	public Tab createTab(String title, Node content) {
		return createTab(title, title, content);
	}

	/**
	 * Creates a new tab.
	 *
	 * @param key
	 * 		Tab key.
	 * @param title
	 * 		Tab title.
	 * @param content
	 * 		Tab content.
	 *
	 * @return Created tab.
	 */
	public Tab createTab(String key, String title, Node content) {
		Tab tab = new KeyedTab(key, title, content);
		add(tab);
		return tab;
	}

	/**
	 * Creates a new tab that cannot be closed.
	 *
	 * @param title
	 * 		Tab title.
	 * @param content
	 * 		Tab content.
	 *
	 * @return Created tab.
	 */
	public Tab createLockedTab(String title, Node content) {
		Tab tab = new KeyedTab(title, content);
		tab.setClosable(false);
		add(tab);
		return tab;
	}

	private void decorateTab(Tab tab, ItemInfo info) {
		// Skip if already decorated
		if (tab.getContextMenu() != null)
			return;
		// Setup tab graphic
		if (info instanceof CommonClassInfo) {
			tab.setGraphic(Icons.getClassIcon((CommonClassInfo) info));
		} else if (info instanceof FileInfo) {
			tab.setGraphic(Icons.getFileIcon((FileInfo) info));
		}
		// Cleanup anything when the tab is closed
		tab.setOnClosed(e -> {
			if (tab.getContent() instanceof Cleanable)
				((Cleanable) tab.getContent()).cleanup();
		});
		// Setup the context menu
		ContextMenu menu = new ContextMenu();
		if (info instanceof CommonClassInfo && tab.getContent() instanceof ClassView) {
			ClassView view = (ClassView) tab.getContent();
			Menu menuMode = Menus.menu("menu.mode");
			menu.getItems().add(menuMode);
			for (ClassViewMode mode : ClassViewMode.values()) {
				MenuItem item = Menus.actionLiteral(mode.toString(), mode.image(), () -> view.setMode(mode));
				menuMode.getItems().add(item);
			}
		} else if (info instanceof FileInfo && tab.getContent() instanceof FileView) {
			FileView view = (FileView) tab.getContent();
			Menu menuMode = Menus.menu("menu.mode");
			menu.getItems().add(menuMode);
			for (FileViewMode mode : FileViewMode.values()) {
				MenuItem item = Menus.actionLiteral(mode.toString(), mode.image(), () -> view.setMode(mode));
				menuMode.getItems().add(item);
			}
		}
		menu.getItems().addAll(
				new SeparatorMenuItem(),
				new ActionMenuItem(Lang.get("menu.tab.close"), () -> {
					TabPane tabPane = tab.getTabPane();
					tabPane.getTabs().remove(tab);
					Event.fireEvent(tab, new Event(Tab.CLOSED_EVENT));
				}),
				new ActionMenuItem(Lang.get("menu.tab.closeothers"), () -> {
					TabPane tabPane = tab.getTabPane();
					tabPane.getTabs().removeAll(tabPane.getTabs().stream()
							.filter(t -> !tab.equals(t))
							.collect(Collectors.toList()));
				}),
				new ActionMenuItem(Lang.get("menu.tab.closeall"), () -> {
					TabPane tabPane = tab.getTabPane();
					List<Tab> oldTabs = new ArrayList<>(tabPane.getTabs());
					tabPane.getTabs().clear();
					oldTabs.forEach(e -> Event.fireEvent(tab, new Event(Tab.CLOSED_EVENT)));
				}),
				new SeparatorMenuItem(),
				new ActionMenuItem(Lang.get("menu.tab.copypath"), () -> {
					ClipboardContent content = new ClipboardContent();
					content.putString(info.getName());
					Clipboard.getSystemClipboard().setContent(content);
				})
		);
		tab.setContextMenu(menu);
	}

	/**
	 * @return All open tracked tabs.
	 */
	public List<Tab> getAllTabs() {
		return new ArrayList<>(titleToTab.values());
	}

	/**
	 * @param tabPane
	 * 		Tab pane to add to the recent history.
	 */
	public void pushRecentTabPane(DetachableTabPane tabPane) {
		recentTabPanes.remove(tabPane);
		recentTabPanes.push(tabPane);
	}

	/**
	 * @return Most recent tab pane.
	 */
	public DetachableTabPane peekRecentTabPane() {
		if (recentTabPanes.isEmpty())
			return null;
		return recentTabPanes.peek();
	}

	/**
	 * @param tabPane
	 * 		Tab pane to remove from the recent history.
	 */
	public void removeRecentTabPane(DetachableTabPane tabPane) {
		if (!recentTabPanes.isEmpty())
			recentTabPanes.remove(tabPane);
	}

	/**
	 * A {@link DetachableTabPane} factory that ensures newly made {@link DetachableTabPane} instances update the
	 * {@link #titleToTab title to tab lookup} and {@link #recentTabPanes}  most recently interacted} with tab pane.
	 */
	private class DockingTabPaneFactory extends DetachableTabPaneFactory {
		@Override
		protected void init(DetachableTabPane newTabPane) {
			newTabPane.setStageFactory(TabStageExt::new);
			newTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
			newTabPane.getTabs().addListener((ListChangeListener<Tab>) c -> {
				while (c.next()) {
					updateTabLookup(c);
					updateRecentTabPane(newTabPane, c);
					updateStageClosable(newTabPane, c);
				}
			});
			KeybindConfig binds = Configs.keybinds();
			newTabPane.setOnKeyPressed(e -> {
				SelectionModel<?> model = newTabPane.getSelectionModel();
				int selectedIndex = model.getSelectedIndex();
				if (binds.closeTab.match(e)) {
					if (selectedIndex >= 0) {
						newTabPane.getTabs().remove(selectedIndex);
					} else {
						logger.warn("Could not close tab that doesn't exist, index={}", getBaselineOffset());
					}
				}
			});
		}

		/**
		 * Update the associated stage
		 *
		 * @param tabPane
		 * 		Tab pane updated.
		 * @param c
		 * 		Change event.
		 */
		private void updateStageClosable(DetachableTabPane tabPane, ListChangeListener.Change<? extends Tab> c) {
			boolean closable = true;
			for (Tab tab : tabPane.getTabs()) {
				closable &= tab.isClosable();
			}
			// For newly spawned windows (stages) do not let them be closable if they contain a tab that is marked as
			// not closable. The closable tabs of the window can still be closed though.
			Scene scene = tabPane.getScene();
			if (scene == null) {
				return;
			}
			Stage stage = (Stage) scene.getWindow();
			if (!closable && !stage.equals(RecafUI.getWindows().getMainWindow())) {
				stage.setOnCloseRequest(e -> {
					tabPane.getTabs().removeIf(Tab::isClosable);
					e.consume();
				});
			}
		}

		/**
		 * Update the {@link #recentTabPanes recent tab pane} if the change includes added items.
		 * Closing a tab does not count as an interaction since the intention is that if a user adds a tab
		 * to the pane, future tabs should be added to it as well.
		 *
		 * @param tabPane
		 * 		Tab pane interacted with.
		 * @param c
		 * 		Change event.
		 */
		private void updateRecentTabPane(DetachableTabPane tabPane, ListChangeListener.Change<? extends Tab> c) {
			if (c.wasAdded() && c.getAddedSize() > 0) {
				pushRecentTabPane(tabPane);
			} else if (c.wasRemoved() && tabPane.getTabs().isEmpty()) {
				removeRecentTabPane(tabPane);
			}
		}

		/**
		 * Update the lookup of {@link #titleToTab tab titles to tab instances}.
		 *
		 * @param c
		 * 		Change event.
		 */
		private void updateTabLookup(ListChangeListener.Change<? extends Tab> c) {
			if (c.wasAdded()) {
				for (Tab newTab : c.getAddedSubList()) {
					if (newTab instanceof KeyedTab) {
						KeyedTab keyedTab = (KeyedTab) newTab;
						titleToTab.put(keyedTab.key, keyedTab);
					} else {
						throw new IllegalStateException("Generated tabs should be keyed!");
					}
				}
			}
			if (c.wasRemoved()) {
				for (Tab removedTab : c.getRemoved()) {
					if (removedTab instanceof KeyedTab) {
						KeyedTab keyedTab = (KeyedTab) removedTab;
						titleToTab.remove(keyedTab.key, keyedTab);
					} else {
						throw new IllegalStateException("Generated tabs should be keyed!");
					}
				}
			}
		}
	}

	/**
	 * Tab with associated key.
	 */
	public static class KeyedTab extends Tab {
		private final String key;

		/**
		 * @param title
		 * 		Tab title text.
		 * @param content
		 * 		Tab content.
		 */
		public KeyedTab(String title, Node content) {
			this(title, title, content);
		}

		/**
		 * @param key
		 * 		String used as a lookup key for the tab.
		 * @param title
		 * 		Tab title text.
		 * @param content
		 * 		Tab content.
		 */
		public KeyedTab(String key, String title, Node content) {
			super(title, content);
			this.key = key;
		}
	}

	/**
	 * Custom stage extension for items created by dropping tabs outside their current region.
	 */
	private static class TabStageExt extends DetachableTabPane.TabStage {
		public TabStageExt(DetachableTabPane prior, Tab tab) {
			super(prior, tab);
			// No need to add the stylesheets to the stage, the docking framework copies them for us.
			// But we will need to reinstall the listeners
			WindowBase.installListeners(this, getScene().getRoot());
			WindowBase.installGlobalBinds(this);
			WindowBase.installLogo(this);
		}
	}
}
