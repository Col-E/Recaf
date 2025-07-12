package software.coley.recaf.util;

import javafx.beans.binding.StringBinding;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

/**
 * Util for displaying error dialogs.
 *
 * @author Matt Coley
 */
public class ErrorDialogs {
	/**
	 * @param title
	 * 		Title text.
	 * @param header
	 * 		Header text.
	 * @param content
	 * 		Content text.
	 * @param t
	 * 		The error to display.
	 */
	public static void show(String title, String header, String content, Throwable t) {
		FxThreadUtil.run(() -> {
			Alert alert = alert(title, header, content);
			configure(alert, t);
		});
	}

	/**
	 * @param title
	 * 		Title text.
	 * @param header
	 * 		Header text.
	 * @param content
	 * 		Content text.
	 * @param t
	 * 		The error to display.
	 */
	public static void show(StringBinding title, StringBinding header, StringBinding content, Throwable t) {
		FxThreadUtil.run(() -> {
			Alert alert = alert(title, header, content);
			configure(alert, t);
		});
	}

	private static void configure(Alert alert, Throwable t) {
		// Create expandable Exception.
		String exceptionText = StringUtil.traceToString(t);
		TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(true);
		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);

		// Set expandable Exception into the dialog pane.
		alert.getDialogPane().setExpandableContent(textArea);
		alert.getDialogPane().setExpanded(true);
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Icons.getImage(Icons.LOGO));
		alert.showAndWait();
	}

	private static Alert alert(StringBinding title, StringBinding header, StringBinding content) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.titleProperty().bind(title);
		alert.headerTextProperty().bind(header);
		alert.contentTextProperty().bind(content);
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Icons.getImage(Icons.LOGO));
		return alert;
	}

	private static Alert alert(String title, String header, String content) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Icons.getImage(Icons.LOGO));
		return alert;
	}
}
