package me.coley.recaf.ui.dialog;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import me.coley.recaf.ui.panel.Wizard;

/**
 * Wraps a {@link Wizard} in a dialog.
 *
 * @param <R>
 * 		Dialog return type.
 *
 * @author Matt Coley
 */
public class WizardDialog<R> extends Dialog<R> {
	/**
	 * @param title
	 * 		Window title.
	 * @param wizard
	 * 		Wizard content.
	 */
	public WizardDialog(String title, Wizard wizard) {
		setTitle(title);
		getDialogPane().setContent(wizard);
		wizard.setOnCancel(() -> {
			// the dummy button is needed
			getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
			close();
		});
	}
}
