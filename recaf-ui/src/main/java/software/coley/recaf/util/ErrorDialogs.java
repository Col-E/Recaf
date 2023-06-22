package software.coley.recaf.util;

import javafx.beans.binding.StringBinding;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

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
	 * @param ex
	 * 		The error to display.
	 */
	public static void show(String title, String header, String content, Exception ex) {
		FxThreadUtil.run(() -> {
			Alert alert = alert(title, header, content);
			configure(alert, ex);
		});
	}

	/**
	 * @param title
	 * 		Title text.
	 * @param header
	 * 		Header text.
	 * @param content
	 * 		Content text.
	 * @param ex
	 * 		The error to display.
	 */
	public static void show(StringBinding title, StringBinding header, StringBinding content, Exception ex) {
		FxThreadUtil.run(() -> {
			Alert alert = alert(title, header, content);
			configure(alert, ex);
		});
	}

	private static void configure(Alert alert, Exception ex) {
		// Create expandable Exception.
		String exceptionText = StringUtil.traceToString(ex);
		TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(true);
		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);

		// Set expandable Exception into the dialog pane.
		alert.getDialogPane().setExpandableContent(textArea);
		alert.getDialogPane().setExpanded(true);
		alert.showAndWait();
	}

	private static Alert alert(StringBinding title, StringBinding header, StringBinding content) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.titleProperty().bind(title);
		alert.headerTextProperty().bind(header);
		alert.contentTextProperty().bind(content);
		return alert;
	}

	private static Alert alert(String title, String header, String content) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		return alert;
	}
}
