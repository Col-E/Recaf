package me.coley.recaf.ui.dialog;

import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.window.WindowBase;

/**
 * Base dialog attributes.
 *
 * @param <R>
 * 		Dialog return type.
 *
 * @author Matt Coley
 */
public class DialogBase<R> extends Dialog<R> {
	protected void init() {
		WindowBase.installStyle(getDialogPane().getStylesheets());
		Stage stage = (Stage) getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image(Icons.LOGO));
		// We can discard the result if the type is a button (like cancel)
		setResultConverter(buttonType -> null);
	}
}
