package me.coley.recaf.ui.dialog;

import javafx.scene.control.ButtonType;

/**
 * Wraps a {@link Wizard} in a dialog.
 *
 * @param <R>
 * 		Dialog return type.
 *
 * @author Matt Coley
 */
public class WizardDialog<R> extends DialogBase<R> {
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
		init();
	}
}
