package me.coley.recaf.ui.controls;

import javafx.application.Platform;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.mapping.Mappings;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.workspace.Workspace;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A popup textfield for renaming classes and members.
 *
 * @author Matt
 */
public class RenamingTextField extends PopupWindow {
	private final GuiController controller;
	private final TextField text;
	private Supplier<Map<String, String>> mapSupplier;
	private Consumer<Map<String, String>> onRename;

	private RenamingTextField(GuiController controller, String initialText, Consumer<RenamingTextField> renameAction) {
		this.controller = controller;
		setHideOnEscape(true);
		setAutoHide(true);
		setOnShown(e -> {
			// Disable root so key events do not get passed to the window that owns the rename popup.
			controller.windows().getMainWindow().getRoot().setDisable(true);
			// Center on main window
			Stage main = controller.windows().getMainWindow().getStage();
			int x = (int) (main.getX() + Math.round((main.getWidth() / 2) - (getWidth() / 2)));
			int y = (int) (main.getY() + Math.round((main.getHeight() / 2) - (getHeight() / 2)));
			setX(x);
			setY(y);
		});
		setOnHiding(e -> {
			// Re-enable root after completion/cancellation
			controller.windows().getMainWindow().getRoot().setDisable(false);
		});
		text = new TextField(initialText);
		text.getStyleClass().add("remap-field");
		text.setPrefWidth(400);
		// Close on hitting escape/close-window bind
		text.setOnKeyPressed(e -> {
			if (controller.config().keys().closeWindow.match(e) || e.getCode() == KeyCode.ESCAPE)
				hide();
		});
		// Set on-enter action
		text.setOnAction(e -> renameAction.accept(this));
		// Setup & show
		getScene().setRoot(text);
		Platform.runLater(() -> {
			text.requestFocus();
			text.selectAll();
		});
	}

	/**
	 * @return Value of text-field.
	 */
	public String getText() {
		return text.getText();
	}

	/**
	 * @param mapSupplier
	 * 		Mapping generator.
	 */
	public void setMapSupplier(Supplier<Map<String, String>> mapSupplier) {
		this.mapSupplier = mapSupplier;
	}

	/**
	 * @param onRename
	 * 		Action to run on the mappings.
	 */
	public void setOnRename(Consumer<Map<String, String>> onRename) {
		this.onRename = onRename;
	}

	/**
	 * Create a renaming field for classes.
	 *
	 * @param controller
	 * 		Controller to act on.
	 * @param name
	 * 		Class name.
	 *
	 * @return Renaming field popup.
	 */
	public static RenamingTextField forClass(GuiController controller, String name) {
		RenamingTextField popup = new RenamingTextField(controller, name, RenamingTextField::defaultAction);
		// Set map supplier for class renaming
		popup.setMapSupplier(() -> {
			String renamed = popup.getText();
			Map<String, String> map = new HashMap<>();
			map.put(name, renamed);
			// Map inners as well
			String prefix = name + "$";
			controller.getWorkspace().getPrimaryClassNames().stream()
					.filter(n -> n.startsWith(prefix))
					.forEach(n -> map.put(n, renamed + n.substring(name.length())));
			return map;
		});
		// Close class tabs with old names & open the new ones
		popup.setOnRename((renamed) -> renamed.forEach((oldName, newName) -> {
			// Get old tab index
			Tab tab = controller.windows().getMainWindow().getTabs().getTab(oldName);
			if (tab == null)
				return;
			int oldIndex = controller.windows().getMainWindow().getTabs().getTabs().indexOf(tab);
			if (oldIndex == -1)
				return;
			// Close old tab
			controller.windows().getMainWindow().getTabs().closeTab(oldName);
			// Open new tab and move to old index
			controller.windows().getMainWindow().openClass(controller.getWorkspace().getPrimary(), newName);
			tab = controller.windows().getMainWindow().getTabs().getTab(newName);
			controller.windows().getMainWindow().getTabs().getTabs().remove(tab);
			controller.windows().getMainWindow().getTabs().getTabs().add(oldIndex, tab);
			controller.windows().getMainWindow().getTabs().select(tab);
		}));
		return popup;
	}

	/**
	 * Create a renaming field for packages.
	 *
	 * @param controller
	 * 		Controller to act on.
	 * @param name
	 * 		Package name.
	 *
	 * @return Renaming field popup.
	 */
	public static RenamingTextField forPackage(GuiController controller, String name) {
		RenamingTextField popup = new RenamingTextField(controller, name, RenamingTextField::defaultAction);
		// Set map supplier for package renaming
		popup.setMapSupplier(() -> {
			String renamed = popup.getText();
			Map<String, String> map = new HashMap<>();
			// Map all classes in the package
			String prefix = name + "/";
			controller.getWorkspace().getPrimaryClassNames().stream()
					.filter(n -> n.startsWith(prefix))
					.forEach(n -> map.put(n, renamed + "/" + n.substring(name.length() + 1)));
			return map;
		});
		// Close class tabs with old names & open the new ones
		popup.setOnRename((renamed) -> renamed.forEach((oldName, newName) -> {
			// Get old tab index
			Tab tab = controller.windows().getMainWindow().getTabs().getTab(oldName);
			if (tab == null)
				return;
			int oldIndex = controller.windows().getMainWindow().getTabs().getTabs().indexOf(tab);
			if (oldIndex == -1)
				return;
			// Close old tab
			controller.windows().getMainWindow().getTabs().closeTab(oldName);
			// Open new tab and move to old index
			controller.windows().getMainWindow().openClass(controller.getWorkspace().getPrimary(), newName);
			tab = controller.windows().getMainWindow().getTabs().getTab(newName);
			controller.windows().getMainWindow().getTabs().getTabs().remove(tab);
			controller.windows().getMainWindow().getTabs().getTabs().add(oldIndex, tab);
			controller.windows().getMainWindow().getTabs().select(tab);
		}));
		return popup;
	}

	/**
	 * Create a renaming field for members.
	 *
	 * @param controller
	 * 		Controller to act on.
	 * @param owner
	 * 		Member's defining class name.
	 * @param name
	 * 		Member name.
	 * @param desc
	 * 		Member descriptor.
	 *
	 * @return Renaming field popup.
	 */
	public static RenamingTextField forMember(GuiController controller, String owner, String name, String desc) {
		RenamingTextField popup = new RenamingTextField(controller, name, RenamingTextField::defaultAction);
		// Set map supplier for member renaming
		popup.setMapSupplier(() -> {
			Map<String, String> map = new HashMap<>();
			boolean isMethod = desc.contains("(");
			if(isMethod) {
				// Method references should be renamed for the entier hierarchy
				controller.getWorkspace().getHierarchyGraph().getHierarchyNames(owner)
						.forEach(hierarchyMember -> map.put(hierarchyMember + "." + name + desc, popup.getText()));
			} else {
				// Field references may not be based on the direct class they are declared in.
				// A child class may refer to a parent class member, using the child class as an owner.
				// However, once a child class introduces a shadowing field name, we want to stop introducing
				// children as owners for this mapping run.
				map.put(owner + "." + name + " " + desc, popup.getText());
				Workspace workspace = controller.getWorkspace();
				workspace.getHierarchyGraph()
						.getAllDescendantsWithBreakCondition(owner,
								n -> ClassUtil.containsField(workspace.getClassReader(n), name, desc))
						.forEach(childOwner -> map.put(childOwner + "." + name, popup.getText()));
			}
			return map;
		});
		// Close class tab with old name & open the new one
		popup.setOnRename(renamed -> {
			// Get old tab index, skipping of the owner class is not currently open
			Tab tab = controller.windows().getMainWindow().getTabs().getTab(owner);
			if (tab == null)
				return;
			int oldIndex = controller.windows().getMainWindow().getTabs().getTabs().indexOf(tab);
			if (oldIndex == -1)
				return;
			// Update display
			ClassViewport viewport =
					controller.windows().getMainWindow().openClass(controller.getWorkspace().getPrimary(), owner);
			viewport.updateView();
		});
		return popup;
	}

	/**
	 * Create a renaming field for files.
	 *
	 * @param controller
	 * 		Controller to act on.
	 * @param name
	 * 		File name.
	 *
	 * @return Renaming field popup.
	 */
	public static RenamingTextField forFile(GuiController controller, String name) {
		RenamingTextField popup = new RenamingTextField(controller, name, RenamingTextField::fileAction);
		// Set map supplier for file renaming
		popup.setMapSupplier(() -> {
			String renamed = popup.getText();
			Map<String, String> map = new HashMap<>();
			map.put(name, renamed);
			return map;
		});
		// Close class tabs with old names & open the new ones
		popup.setOnRename((renamed) -> renamed.forEach((oldName, newName) -> {
			// Get old tab index
			Tab tab = controller.windows().getMainWindow().getTabs().getTab(oldName);
			if (tab == null)
				return;
			int oldIndex = controller.windows().getMainWindow().getTabs().getTabs().indexOf(tab);
			if (oldIndex == -1)
				return;
			// Close old tab
			controller.windows().getMainWindow().getTabs().closeTab(oldName);
			// Open new tab and move to old index
			controller.windows().getMainWindow().openFile(controller.getWorkspace().getPrimary(), newName);
			tab = controller.windows().getMainWindow().getTabs().getTab(newName);
			controller.windows().getMainWindow().getTabs().getTabs().remove(tab);
			controller.windows().getMainWindow().getTabs().getTabs().add(oldIndex, tab);
			controller.windows().getMainWindow().getTabs().select(tab);
		}));
		return popup;
	}

	private static void fileAction(RenamingTextField field) {
		// Apply mappings
		Map<String, String> map = field.mapSupplier.get();
		Map<String, byte[]> files = field.controller.getWorkspace().getPrimary().getFiles();
		map.forEach((oldName, newName) -> {
			byte[] tmp = files.remove(oldName);
			if (tmp != null)
				files.put(newName, tmp);
		});
		// Close popup
		field.hide();
		field.onRename.accept(map);
	}

	private static void defaultAction(RenamingTextField field) {
		// Apply mappings
		Map<String, String> map = field.mapSupplier.get();
		Mappings mappings = new Mappings(field.controller.getWorkspace());
		mappings.setMappings(map);
		mappings.accept(field.controller.getWorkspace().getPrimary());
		// Refresh affected tabs
		ViewportTabs tabs = field.controller.windows().getMainWindow().getTabs();
		for (String updated : field.controller.getWorkspace().getDefinitionUpdatedClasses()) {
			if (tabs.isOpen(updated)) {
				tabs.getClassViewport(updated).updateView();
			}
		}
		// Close popup
		field.hide();
		field.onRename.accept(map);
	}
}
