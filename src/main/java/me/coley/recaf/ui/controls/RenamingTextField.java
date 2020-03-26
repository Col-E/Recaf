package me.coley.recaf.ui.controls;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.mapping.Mappings;
import me.coley.recaf.ui.controls.view.ClassViewport;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A popup textfield for renaming classes and members.
 *
 * @author Matt
 */
public class RenamingTextField extends PopupWindow {
	private final TextField text;
	private Supplier<Map<String, String>> mapSupplier;

	private RenamingTextField(GuiController controller, String initialText) {
		setHideOnEscape(true);
		setAutoHide(true);
		setOnShown(e -> {
			// Center on main window
			Stage main = controller.windows().getMainWindow().getStage();
			int x = (int) (main.getX() + Math.round((main.getWidth() / 2) - (getWidth() / 2)));
			int y = (int) (main.getY() + Math.round((main.getHeight() / 2) - (getHeight() / 2)));
			setX(x);
			setY(y);
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
		text.setOnAction(e -> {
			// TODO: Verify name is valid
			// Apply mappings
			Map<String, String> map = mapSupplier.get();
			Mappings mappings = new Mappings(controller.getWorkspace());
			mappings.setMappings(map);
			mappings.accept(controller.getWorkspace().getPrimary());
			// Close popup
			hide();
		});
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
		RenamingTextField popup = new RenamingTextField(controller, name);
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
		// Close class tab with old name & open thegt new one
		popup.setOnHiding(e -> {
			controller.windows().getMainWindow().getTabs().closeTab(name);
			controller.windows().getMainWindow().openClass(controller.getWorkspace().getPrimary(), popup.getText());
		});
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
		RenamingTextField popup = new RenamingTextField(controller, name);
		// Set map supplier for member renaming
		popup.setMapSupplier(() -> {
			Map<String, String> map = new HashMap<>();
			String member = desc.contains("(") ? name + desc : name;
			controller.getWorkspace().getHierarchyGraph().getHierarchyNames(owner)
					.forEach(hierarchyMember -> map.put(hierarchyMember + "." + member, popup.getText()));
			return map;
		});
		// Close class tab with old name & open thegt new one
		popup.setOnHiding(e -> {
			ClassViewport viewport = controller.windows().getMainWindow()
					.openClass(controller.getWorkspace().getPrimary(), owner);
			viewport.updateView();
		});
		return popup;
	}
}
