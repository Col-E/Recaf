package me.coley.recaf.ui.component;

import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import me.coley.recaf.util.Icons;
import me.coley.recaf.util.Lang;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.Optional;

public class UpdatePrompt {
	public static boolean consent(String current, String latest, String markdown) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle(Lang.get("update.outdated"));
		alert.setContentText(Lang.get("update.consent"));
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Icons.LOGO);

		Parser parser = Parser.builder().build();
		Node document = parser.parse(markdown);
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		String html = renderer.render(document);
		WebView browser = new WebView();
		WebEngine webEngine = browser.getEngine();
		ScrollPane scrollPane = new ScrollPane(browser);
		String css = "* { font-family: Arial, sans-serif; }";
		html = "<head><style>" + css + "</style></head><h1>" + current + " &#8594; " + latest + "<h1>\n<hr>\n" + html;
		webEngine.loadContent(html, "text/html");
		alert.setGraphic(scrollPane);

		Optional<ButtonType> result = alert.showAndWait();
		return result.get() == ButtonType.OK;
	}
}
