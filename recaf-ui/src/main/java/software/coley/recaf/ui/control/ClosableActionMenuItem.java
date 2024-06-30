package software.coley.recaf.ui.control;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.NodeEvents;
import software.coley.recaf.util.threading.ThreadUtil;

/**
 * MenuItem with an on-click and on-close runnable action.
 *
 * @author Wolfie / win32kbase
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
		Label label = new Label(text);
		label.setPadding(new Insets(10, 5, 10, 0));
		Button closeButton = new ActionButton(new FontIconView(CarbonIcons.CLOSE), () -> {
			Menu parent = getParentMenu();
			if (parent != null)
				parent.getItems().remove(this);
			onClose.run();

			// Mark as disabled to show that the closure has been processed.
			// We can't instantly refresh the menu, so this is as good as we can do.
			setDisable(true);
		});
		closeButton.getStyleClass().addAll(Styles.RIGHT_PILL);
		closeButton.prefWidthProperty().bind(closeButton.heightProperty());
		getStyleClass().add("closable-menu-item");

		// Layout
		HBox box = new HBox();
		box.setSpacing(10);
		box.setAlignment(Pos.CENTER_LEFT);
		box.getChildren().addAll(closeButton, graphic, label);
		setContent(box);

		// Hack to make the box fill the menu width.
		//  - When we are added to a menu...
		//    - And the menu is shown...
		//      - Initially show the precomputed size for items...
		//        - But then use those sizes of all items to figure the max width and set that for this (all) boxes
		NodeEvents.runOnceOnChange(parentMenuProperty(), parent -> {
			NodeEvents.dispatchAndRemoveIf(parent.showingProperty(), showing -> {
				if (showing) {
					box.setPrefWidth(Region.USE_COMPUTED_SIZE);
					FxThreadUtil.delayedRun(1, () -> {
						double size = parent.getItems().stream()
								.filter(i -> i instanceof CustomMenuItem)
								.map(i -> ((CustomMenuItem) i).getContent())
								.mapToDouble(n -> n.getBoundsInParent().getWidth())
								.max().orElse(100);
						double max = Math.max(100, size);
						box.setPrefWidth(max);
					});
				}
				return false;
			});
		});

		// With 'setOnAction(...)' the action is run on the JFX thread.
		// We want the actions to be run on background threads so the UI does not hang on long-running tasks.
		setOnAction(e -> ThreadUtil.run(action));
	}


}