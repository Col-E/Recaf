package me.coley.recaf.ui.dialog;

import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;

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
		getDialogPane().getStylesheets().add("style/base.css");
		getDialogPane().getStylesheets().add("style/scroll.css");
		getDialogPane().getStylesheets().add("style/tree.css");
		Stage stage = (Stage) getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image("icons/logo.png"));
	}
}
