package me.coley.recaf.ui.dialog;

import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import me.coley.recaf.ui.control.BoundLabel;
import me.coley.recaf.ui.util.Lang;

import java.util.Stack;

/**
 * Wizard panel.
 *
 * @author Jewelsea - https://stackoverflow.com/a/19201342
 * @author Matt Coley - Slight modifications
 */
public class Wizard extends StackPane {
	private static final int UNDEFINED = -1;
	private final ObservableList<WizardPage> pages = FXCollections.observableArrayList();
	private final Stack<Integer> history = new Stack<>();
	private int currentPage = UNDEFINED;
	private Runnable onFinish;
	private Runnable onCancel;

	/**
	 * @param nodes
	 * 		Pages of the wizard to show.
	 */
	public Wizard(WizardPage... nodes) {
		pages.addAll(nodes);
		navTo(0);
	}

	void nextPage() {
		if (hasNextPage()) {
			navTo(currentPage + 1);
		}
	}

	void priorPage() {
		if (hasPriorPage()) {
			navTo(history.pop(), false);
		}
	}

	boolean hasNextPage() {
		return (currentPage < pages.size() - 1);
	}

	boolean hasPriorPage() {
		return !history.isEmpty();
	}

	void navTo(int nextPageIdx, boolean pushHistory) {
		if (nextPageIdx < 0 || nextPageIdx >= pages.size()) return;
		if (currentPage != UNDEFINED) {
			if (pushHistory) {
				history.push(currentPage);
			}
		}

		WizardPage nextPage = pages.get(nextPageIdx);
		currentPage = nextPageIdx;
		getChildren().clear();
		getChildren().add(nextPage);
		nextPage.manageButtons();
	}

	void navTo(int nextPageIdx) {
		navTo(nextPageIdx, true);
	}

	void navTo(String id) {
		Node page = lookup("#" + id);
		if (page != null) {
			int nextPageIdx = pages.indexOf(page);
			if (nextPageIdx != UNDEFINED) {
				navTo(nextPageIdx);
			}
		}
	}

	/**
	 * @param onFinish
	 * 		Action to run when wizard completes.
	 */
	public void setOnFinish(Runnable onFinish) {
		this.onFinish = onFinish;
	}

	/**
	 * @param onCancel
	 * 		Action to run when wizard cancels.
	 */
	public void setOnCancel(Runnable onCancel) {
		this.onCancel = onCancel;
	}

	private void finish() {
		if (onFinish != null)
			onFinish.run();
	}

	private void cancel() {
		if (onCancel != null)
			onCancel.run();
	}

	/**
	 * Wizard page content.
	 */
	public abstract static class WizardPage extends VBox {
		private final Button priorButton = newButton("dialog.previous");
		private final Button nextButton = newButton("dialog.next");
		private final Button cancelButton = newButton("dialog.cancel");
		private final Button finishButton = newButton("dialog.finish");
		private boolean isFinal;

		/**
		 * @param title
		 * 		Page title text.
		 * @param isFinal
		 * 		Is last page.
		 */
		public WizardPage(StringBinding title, boolean isFinal) {
			this.isFinal = isFinal;

			setSpacing(5);

			Label lblTitle = new BoundLabel(title);
			lblTitle.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 5 0;");
			idProperty().bind(title);

			Region spring = new Region();
			VBox.setVgrow(spring, Priority.ALWAYS);
			getChildren().addAll(lblTitle, getContent(), spring, createButtons());

			priorButton.setOnAction(actionEvent -> priorPage());
			nextButton.setOnAction(actionEvent -> nextPage());
			cancelButton.setOnAction(actionEvent -> getWizard().cancel());
			finishButton.setOnAction(actionEvent -> getWizard().finish());
			cancelButton.setCancelButton(true);
			finishButton.setDefaultButton(true);
		}

		HBox createButtons() {
			Region spring = new Region();
			HBox.setHgrow(spring, Priority.ALWAYS);
			HBox buttonBar = new HBox(5);
			if (isFinal) {
				buttonBar.getChildren().addAll(spring, priorButton, finishButton, cancelButton);
			} else {
				buttonBar.getChildren().addAll(spring, priorButton, nextButton, cancelButton);
			}
			return buttonBar;
		}

		protected void setIsFinal(boolean isFinal) {
			if (isFinal != this.isFinal) {
				this.isFinal = isFinal;
				// Recreate button options
				getChildren().remove(getChildren().size() - 1);
				getChildren().add(createButtons());
			}
		}

		protected abstract Parent getContent();

		boolean hasNextPage() {
			return getWizard().hasNextPage();
		}

		boolean hasPriorPage() {
			return getWizard().hasPriorPage();
		}

		void nextPage() {
			getWizard().nextPage();
		}

		void priorPage() {
			getWizard().priorPage();
		}

		void navTo(String id) {
			getWizard().navTo(id);
		}

		Wizard getWizard() {
			return (Wizard) getParent();
		}

		void manageButtons() {
			if (!hasPriorPage()) {
				priorButton.setDisable(true);
			}

			if (!hasNextPage()) {
				nextButton.setDisable(true);
			}
		}

		private static Button newButton(String key) {
			Button button = new Button();
			button.textProperty().bind(Lang.getBinding(key));
			return button;
		}
	}
}
