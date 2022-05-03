package me.coley.recaf.ui.dialog;

import javafx.beans.binding.StringBinding;
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
	private static final int MIN_WIDTH = 350;

	/**
	 * @param wizard
	 * 		Wizard content.
	 */
	private WizardDialog(Wizard wizard) {
		setResizable(true);
		getDialogPane().setContent(wizard);
		getDialogPane().setMinWidth(MIN_WIDTH);
		wizard.setOnCancel(() -> {
			// the dummy button is needed
			getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
			close();
		});
		init();
	}

	/**
	 * @param title
	 * 		Window title.
	 * @param wizard
	 * 		Wizard content.
	 */
	public WizardDialog(String title, Wizard wizard) {
		this(wizard);
		setTitle(title);
	}

	/**
	 * @param title
	 * 		Window title.
	 * @param wizard
	 * 		Wizard content.
	 */
	public WizardDialog(StringBinding title, Wizard wizard) {
		this(wizard);
		titleProperty().bind(title);
	}
}
