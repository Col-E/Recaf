package me.coley.recaf.ui.dialog;

import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;
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
		WindowBase.addStylesheets(getDialogPane().getStylesheets());
		Stage stage = (Stage) getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image("icons/logo.png"));
	}
}
