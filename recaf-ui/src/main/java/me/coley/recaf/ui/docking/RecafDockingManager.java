package me.coley.recaf.ui.docking;

import com.panemu.tiwulfx.control.dock.DetachableTabPane;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.ClassView;
import me.coley.recaf.ui.ClassViewMode;
import me.coley.recaf.ui.FileView;
import me.coley.recaf.ui.FileViewMode;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.control.menu.ActionMenuItem;
import me.coley.recaf.ui.docking.impl.ClassTab;
import me.coley.recaf.ui.docking.impl.FileTab;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.util.Menus;
import me.coley.recaf.ui.window.WindowBase;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Recaf specific logic / handling for docking operations.
 *
 * @author Matt Coley
 */
public class RecafDockingManager extends DockingManager {
	private static final RecafDockingManager INSTANCE = new RecafDockingManager();
	private final Map<String, ClassTab> classTabs = new HashMap<>();
	private final Map<String, FileTab> fileTabs = new HashMap<>();
	private final Set<DockTab> miscTabs = new HashSet<>();

	/**
	 * Deny public construction, require {@link #getInstance()}.
	 */
	private RecafDockingManager() {
		setRegionFactory(new RecafRegionFactory(this));
		// We're operating off of content being 'class/file-view' rather than the tab being 'class/file-tab'
		// because things like hierarchy panes are class tabs, but not what you use for primary editing.
		// So for those sorts of tabs we do not want to track them as the 'open' class/file.
		//
		// In the future we will probably support having multiple tabs open of the same class/file, but that's
		// a challenge for another day.
		addTabCreationListener(tab -> {
			Node content = tab.getContent();
			if (content instanceof ClassView) {
				String path = ((ClassTab) tab).getClassRepresentation().getCurrentClassInfo().getName();
				classTabs.put(path, (ClassTab) tab);
			} else if (content instanceof FileView) {
				String path = ((FileTab) tab).getFileRepresentation().getCurrentFileInfo().getName();
				fileTabs.put(path, (FileTab) tab);
			} else {
				miscTabs.add(tab);
			}
		});
		addTabClosureListener(tab -> {
			Node content = tab.getContent();
			if (content instanceof ClassView) {
				String path = ((ClassTab) tab).getClassRepresentation().getCurrentClassInfo().getName();
				classTabs.remove(path);
			} else if (content instanceof FileView) {
				String path = ((FileTab) tab).getFileRepresentation().getCurrentFileInfo().getName();
				classTabs.remove(path);
			} else {
				miscTabs.remove(tab);
			}
		});
	}

	/**
	 * @return Map of classes currently open and their representing tabs.
	 */
	public Map<String, ClassTab> getClassTabs() {
		return classTabs;
	}

	/**
	 * @return Map of files currently open and their representing tabs.
	 */
	public Map<String, FileTab> getFileTabs() {
		return fileTabs;
	}

	/**
	 * @return Set of currently open non class or file tabs.
	 */
	public Set<DockTab> getMiscTabs() {
		return miscTabs;
	}

	/**
	 * @return Docking manager instance.
	 */
	public static RecafDockingManager getInstance() {
		return INSTANCE;
	}

	@Override
	protected DockTabFactory decorateFactory(DockingRegion region, DockTabFactory factory) {
		DockTab tab = factory.get();
		Node content = tab.getContent();
		// Update graphics and context menus to match tab content
		ContextMenu menu = new ContextMenu();
		ItemInfo info = null;
		if (tab instanceof ClassTab) {
			ClassTab classTab = (ClassTab) tab;
			CommonClassInfo classInfo = classTab.getClassRepresentation().getCurrentClassInfo();
			tab.setGraphic(Icons.getClassIcon(classInfo));
			info = classInfo;
			if (content instanceof ClassView) {
				Menu menuMode = Menus.menu("menu.mode");
				menu.getItems().add(menuMode);
				ClassView view = (ClassView) content;
				for (ClassViewMode mode : ClassViewMode.values()) {
					MenuItem item = Menus.actionLiteral(mode.toString(), mode.image(), () -> view.setMode(mode));
					menuMode.getItems().add(item);
				}
			}
		} else if (tab instanceof FileTab) {
			FileTab fileTab = (FileTab) tab;
			FileInfo fileInfo = fileTab.getFileRepresentation().getCurrentFileInfo();
			tab.setGraphic(Icons.getFileIcon(fileInfo));
			info = fileInfo;
			if (content instanceof FileView) {
				Menu menuMode = Menus.menu("menu.mode");
				menu.getItems().add(menuMode);
				FileView view = (FileView) content;
				for (FileViewMode mode : FileViewMode.values()) {
					MenuItem item = Menus.actionLiteral(mode.toString(), mode.image(), () -> view.setMode(mode));
					menuMode.getItems().add(item);
				}
			}
		}
		// Standard context menu items
		menu.getItems().addAll(
				new SeparatorMenuItem(),
				new ActionMenuItem(Lang.getBinding("menu.tab.close"), () -> {
					TabPane tabPane = tab.getTabPane();
					tabPane.getTabs().remove(tab);
					tab.close();
				}),
				new ActionMenuItem(Lang.getBinding("menu.tab.closeothers"), () -> {
					TabPane tabPane = tab.getTabPane();
					tabPane.getTabs().removeAll(tabPane.getTabs().stream()
							.filter(t -> !tab.equals(t))
							.collect(Collectors.toList()));
				}),
				new ActionMenuItem(Lang.getBinding("menu.tab.closeall"), () -> {
					TabPane tabPane = tab.getTabPane();
					List<Tab> oldTabs = new ArrayList<>(tabPane.getTabs());
					tabPane.getTabs().clear();
					oldTabs.forEach(e -> tab.close());
				}));
		if (info != null) {
			String infoPath = info.getName();
			menu.getItems().addAll(new SeparatorMenuItem(),
					new ActionMenuItem(Lang.getBinding("menu.tab.copypath"), () -> {
						ClipboardContent clipboard = new ClipboardContent();
						clipboard.putString(infoPath);
						Clipboard.getSystemClipboard().setContent(clipboard);
					}));
		}
		// Cleanup anything when the tab is closed
		if (content instanceof Cleanable) {
			tab.setOnClosed(e -> {
				if (tab.getContent() instanceof Cleanable)
					((Cleanable) tab.getContent()).cleanup();
			});
		}
		return () -> tab;
	}

	/**
	 * {@link RegionFactory} implementation to set the stage factory to produce {@link RecafTabStage}.
	 */
	private static class RecafRegionFactory extends RegionFactory {
		/**
		 * @param manager
		 * 		Associated manager.
		 */
		public RecafRegionFactory(DockingManager manager) {
			super(manager);
		}

		@Override
		protected void init(DetachableTabPane tabPane) {
			super.init(tabPane);
			tabPane.setStageFactory(RecafTabStage::new);
			tabPane.setOnKeyPressed(e -> {
				SelectionModel<?> model = tabPane.getSelectionModel();
				int selectedIndex = model.getSelectedIndex();
				if (Configs.keybinds().closeTab.match(e)) {
					if (selectedIndex >= 0) {
						DockTab tab = (DockTab) tabPane.getTabs().get(selectedIndex);
						if (tab.isClosable())
							tab.close();
					}
				}
			});
		}
	}

	/**
	 * Custom stage extension for items created by dropping tabs outside their current region.
	 */
	private static class RecafTabStage extends DetachableTabPane.TabStage {
		public RecafTabStage(DetachableTabPane prior, Tab tab) {
			super(prior, tab);
			// No need to add the stylesheets to the stage, the docking framework copies them for us.
			// But we will need to reinstall the listeners
			WindowBase.installListeners(this, getScene().getRoot());
			WindowBase.installGlobalBinds(this);
			WindowBase.installLogo(this);
		}
	}
}
