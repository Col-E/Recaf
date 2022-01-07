package me.coley.recaf.ui.controls;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import me.coley.recaf.util.UiUtil;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;

import static me.coley.recaf.util.LangUtil.translate;
import static me.coley.recaf.util.ClasspathUtil.resource;
import static me.coley.recaf.util.Log.error;

/**
 * Alert that shows an exception.
 *
 * @author Matt
 */
public class ExceptionAlert extends Alert {
	private static final String BUG_REPORT_LINK = "https://github.com/Col-E/Recaf/" +
			"issues/new?template=bug_report.md&title=";

	private ExceptionAlert(Throwable t, String msg) {
		super(AlertType.ERROR);
		setTitle("An error occurred");
		String header = t.getMessage();
		if(header == null)
			header = "(" + translate("ui.noerrormsg") + ")";
		setHeaderText(header);
		setContentText(msg);
		// Get exception text
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		String exText = sw.toString();
		// Create expandable Exception.
		Label lbl = new Label("Exception stacktrace:");
		TextArea txt = new TextArea(exText);
		txt.setEditable(false);
		txt.setWrapText(false);
		txt.setMaxWidth(Double.MAX_VALUE);
		txt.setMaxHeight(Double.MAX_VALUE);
		Hyperlink link = new Hyperlink("[Bug report]");
		link.setOnAction(e -> {
			try {
				// Append suffix to bug report url for the issue title
				String suffix = t.getClass().getSimpleName();
				if (t.getMessage() != null)
					suffix = suffix + ": " + t.getMessage();
				suffix = URLEncoder.encode(suffix, "UTF-8");
				// Open link in default browser
				UiUtil.showDocument(URI.create(BUG_REPORT_LINK + suffix));
			} catch(IOException exx) {
				error(exx, "Failed to open bug report link");
				show(exx, "Failed to open bug report link.");
			}
		});
		TextFlow prompt = new TextFlow(new Text(translate("ui.openissue")), link);
		GridPane.setVgrow(txt, Priority.ALWAYS);
		GridPane.setHgrow(txt, Priority.ALWAYS);
		GridPane grid = new GridPane();
		grid.setMaxWidth(Double.MAX_VALUE);
		grid.add(lbl, 0, 0);
		grid.add(txt, 0, 1);
		grid.add(prompt, 0, 2);
		getDialogPane().setExpandableContent(grid);
		getDialogPane().setExpanded(true);
		// Set icon
		Stage stage = (Stage) getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image(resource("icons/error.png")));
	}

	/**
	 * Show the dialog for the given exception.
	 *
	 * @param t
	 * 		Exception to show in the dialog.
	 */
	public static void show(Throwable t) {
		show(t, null);
	}

	/**
	 * Show the dialog for the given exception.
	 *
	 * @param t
	 * 		Exception to show in the dialog.
	 * @param msg
	 * 		Additional message to show.
	 */
	public static void show(Throwable t, String msg) {
		Platform.runLater(() -> new ExceptionAlert(t, msg).show());
	}
}
