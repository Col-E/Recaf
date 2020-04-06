package me.coley.recaf.ui.controls.popup;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import me.coley.recaf.ui.controls.ActionButton;

import static me.coley.recaf.util.LangUtil.translate;

/**
 * Yes no popup.
 *
 * @author Matt
 */
public class YesNoWindow extends DragPopup {
	private YesNoWindow(Node content, Control handle) {
		super(content, handle);
	}

	/**
	 * @param prompt
	 * 		Confirmation prompt text.
	 * @param yesAction
	 * 		Action to run when clicking 'yes'.
	 * @param noAction
	 * 		Action to run when clicking 'no'.
	 *
	 * @return Yes-No option window.
	 */
	public static YesNoWindow prompt(String prompt, Runnable yesAction, Runnable noAction) {
		GridPane grid = new GridPane();
		grid.setPadding(new Insets(15));
		grid.setVgap(4);
		grid.setAlignment(Pos.CENTER);
		grid.setMinWidth(150);
		for(int col = 0; col < 3; col++) {
			ColumnConstraints cc = new ColumnConstraints();
			cc.setHgrow(Priority.ALWAYS);
			cc.setFillWidth(true);
			grid.getColumnConstraints().add(cc);
		}
		grid.add(new Label(prompt), 0, 0, 2, 1);
		// Create window
		Label lblTitle = new Label(translate("misc.confirm"));
		lblTitle.getStyleClass().add("h1");
		YesNoWindow window = new YesNoWindow(grid, lblTitle);
		// Add yes/no buttons
		ActionButton btnYes = new ActionButton(translate("misc.yes"), () -> {
			if (yesAction != null)
				yesAction.run();
			window.close();
		});
		ActionButton btnNo = new ActionButton(translate("misc.no"), () -> {
			if (noAction != null)
				noAction.run();
			window.close();
		});
		btnYes.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		btnNo.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		grid.add(btnYes, 0, 2);
		grid.add(btnNo, 1, 2);
		return window;
	}
}
