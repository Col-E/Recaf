package me.coley.recaf.ui.controls;

import javafx.scene.control.ComboBox;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.workspace.JavaResource;

/**
 * Combo-Box for resources.
 *
 * @author Matt
 */
public class ResourceComboBox extends ComboBox<JavaResource> {
	/**
	 * @param controller
	 * 		Controller with resources.
	 */
	public ResourceComboBox(GuiController controller) {
		getStyleClass().add("resource-selector");
		setCellFactory(e -> new ResourceSelectionCell(controller));
		setMaxWidth(Double.MAX_VALUE);
	}
}
