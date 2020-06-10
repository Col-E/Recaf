package me.coley.recaf.ui.controls.popup;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import me.coley.recaf.Recaf;
import me.coley.recaf.ui.MainWindow;
import me.coley.recaf.ui.controls.ActionButton;
import me.coley.recaf.util.self.SelfUpdater;

import static me.coley.recaf.util.LangUtil.translate;

/**
 * Update popup window.
 *
 * @author Matt
 */
public class UpdateWindow extends DragPopup {
	private UpdateWindow(Node content, Control handle) {
		super(content, handle);
	}

	/**
	 * @param window
	 * 		Window reference to handle UI access.
	 *
	 * @return Update popup.
	 */
	public static UpdateWindow create(MainWindow window) {
		Label lblTitle = new Label(translate("update.outdated"));
		Label lblVersion = new Label(Recaf.VERSION + " → " + SelfUpdater.getLatestVersion());
		Label lblDate = new Label(SelfUpdater.getLatestVersionDate().toString());
		lblTitle.getStyleClass().add("h1");
		lblDate.getStyleClass().add("faint");
		GridPane grid = new GridPane();
		GridPane.setHalignment(lblVersion, HPos.CENTER);
		GridPane.setHalignment(lblDate, HPos.CENTER);
		grid.setPadding(new Insets(15));
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setAlignment(Pos.CENTER);
		grid.add(new Label(translate("update.available")), 0, 0);
		grid.add(new ActionButton(translate("misc.open"), () -> window.getMenubar().showUpdatePrompt()), 1, 0);
		grid.add(lblVersion, 0, 1, 2, 1);
		grid.add(lblDate, 0, 2, 2, 1);
		return new UpdateWindow(grid, lblTitle);
	}
}
