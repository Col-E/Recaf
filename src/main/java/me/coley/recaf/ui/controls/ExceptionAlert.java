package me.coley.recaf.ui.controls;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.awt.Desktop;
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

	private ExceptionAlert(Exception ex, String msg) {
		super(AlertType.ERROR);
		setTitle("An error occurred");
		String header = ex.getMessage();
		if(header == null)
			header = "(" + translate("ui.noerrormsg") + ")";
		setHeaderText(header);
		setContentText(msg);
		// Get exception text
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
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
				String suffix = ex.getClass().getSimpleName();
				if (ex.getMessage() != null)
					suffix = suffix + ": " + ex.getMessage();
				suffix = URLEncoder.encode(suffix, "UTF-8");
				// Open link in default browser
				Desktop.getDesktop().browse(URI.create(BUG_REPORT_LINK + suffix));
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
		// If desktop is supported, allow click-able bug report link
		if (Desktop.isDesktopSupported())
			grid.add(prompt, 0, 2);
		getDialogPane().setExpandableContent(grid);
		getDialogPane().setExpanded(true);
		// Set icon
		Stage stage = (Stage) getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image(resource("icons/log/errr.png")));
	}

	/**
	 * Show the dialog for the given exception.
	 *
	 * @param ex
	 * 		Exception to show in the dialog.
	 */
	public static void show(Exception ex) {
		show(ex, null);
	}

	/**
	 * Show the dialog for the given exception.
	 *
	 * @param ex
	 * 		Exception to show in the dialog.
	 * @param msg
	 * 		Additional message to show.
	 */
	public static void show(Exception ex, String msg) {
		new ExceptionAlert(ex, msg).show();
	}
}
