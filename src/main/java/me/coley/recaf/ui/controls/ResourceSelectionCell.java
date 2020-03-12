package me.coley.recaf.ui.controls;

import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.layout.HBox;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.util.UiUtil;
import me.coley.recaf.workspace.*;


/**
 * Cell/renderer for displaying {@link JavaResource}s.
 */
public class ResourceSelectionCell extends ComboBoxListCell<JavaResource> {
	private final GuiController controller;

	/**
	 * @param controller
	 * 		Controller to use.
	 */
	public ResourceSelectionCell(GuiController controller) {
		this.controller = controller;
	}

	@Override
	public void updateItem(JavaResource item, boolean empty) {
		super.updateItem(item, empty);
		if(!empty) {
			HBox g = new HBox();
			if(item != null) {
				String t = item.toString();
				// Add icon for resource types
				g.getChildren().add(new IconView(UiUtil.getResourceIcon(item)));
				// Indicate which resource is the primary resource
				if(controller.getWorkspace() != null && item == controller.getWorkspace().getPrimary()) {
					Label lbl = new Label(" [Primary]");
					lbl.getStyleClass().add("bold");
					g.getChildren().add(lbl);
				}
				setText(t);
			}
			setGraphic(g);
		} else {
			setGraphic(null);
			setText(null);
		}
	}
}