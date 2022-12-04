package me.coley.recaf.ui.control.menu;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import me.coley.recaf.ui.control.GraphicActionButton;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.menu.MainMenu;
import me.coley.recaf.util.threading.ThreadUtil;

/**
 * MenuItem with an on-click and on-close runnable action.
 *
 * @author Wolfie / win32kbase
 * @see MainMenu#refreshRecent()
 */
public class ClosableActionMenuItem extends CustomMenuItem {
	/**
	 * @param text
	 * 		Menu item text.
	 * @param graphic
	 * 		Menu item graphic.
	 * @param action
	 * 		Action to run on menu item click.
	 * @param onClose
	 * 		Action to run to remove the item from the parent menu.
	 */
	public ClosableActionMenuItem(String text, Node graphic, Runnable action, Runnable onClose) {
		HBox item = new HBox();
		Pane spacer = new Pane();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		item.setAlignment(Pos.CENTER);
		item.setSpacing(6);

		Label label = new Label(text);
		Button closeButton = new GraphicActionButton(Icons.getIconView(Icons.CLOSE), () -> {
			Menu parent = getParentMenu();
			if (parent != null)
				parent.getItems().remove(this);
			onClose.run();
			// Mark as disabled to show that the closure has been processed.
			// We can't instantly refresh the menu, so this is as good as we can do.
			setDisable(true);
		});
		closeButton.prefWidthProperty().bind(closeButton.heightProperty());
		// Layout
		item.getChildren().addAll(closeButton, graphic, label);
		setContent(item);
		// With 'setOnAction(...)' the action is run on the JFX thread.
		// We want the actions to be run on background threads so the UI does not hang on long-running tasks.
		setOnAction(e -> ThreadUtil.run(action));
	}
}