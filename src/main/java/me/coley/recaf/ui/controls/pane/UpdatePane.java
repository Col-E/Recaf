package me.coley.recaf.ui.controls.pane;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.ActionButton;
import me.coley.recaf.ui.controls.ExceptionAlert;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.UiUtil;
import me.coley.recaf.util.self.SelfUpdater;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import static me.coley.recaf.util.LangUtil.translate;

/**
 * Panel that shows update information.
 *
 * @author Matt
 */
public class UpdatePane extends BorderPane {
	private final GuiController controller;

	/**
	 * @param controller
	 * 		Controller to use to close Recaf.
	 */
	public UpdatePane(GuiController controller) {
		this.controller = controller;
		Button btn = getButton();
		GridPane grid = new GridPane();
		GridPane.setFillHeight(btn, true);
		GridPane.setFillWidth(btn, true);
		grid.setMaxWidth(800);
		grid.setPadding(new Insets(10));
		grid.setVgap(10);
		grid.setHgap(10);
		grid.add(getNotes(), 0, 0, 2, 1);
		grid.add(getInfo(), 0, 1, 1, 1);
		grid.add(btn, 1, 1, 1, 1);
		grid.add(getSubMessage(), 0, 2, 2, 1);
		setTop(getHeader());
		setCenter(grid);
	}

	private BorderPane getHeader() {
		Label header = new Label("Recaf: " + SelfUpdater.getLatestVersion());
		header.getStyleClass().add("h1");
		BorderPane pane = new BorderPane(header);
		pane.getStyleClass().add("update-header");
		return pane;
	}

	private BorderPane getNotes() {
		TextFlow flow = new TextFlow();
		Parser parser = Parser.builder().build();
		Node document = parser.parse(SelfUpdater.getLatestPatchnotes());
		document.accept(new AbstractVisitor() {
			@Override
			public void visit(Paragraph paragraph) {
				// Add all content to same line
				parse(paragraph, this::text, this::link);
				newLine();
			}

			@Override
			public void visit(Heading heading) {
				// Skip the version H2 text
				if (heading.getLevel() <= 2)
					return;
				// Extract heading text
				StringBuilder sb = new StringBuilder();
				parse(heading, text -> sb.append(text.getLiteral()), null);
				// Render
				addText(sb.toString(), "h2");
				newLine();
			}

			@Override
			public void visit(BulletList list) {
				Node item = list.getFirstChild();
				do {
					// Prefix with bullet point
					addText(" ● ", null);
					// Add all content to same line
					parse(item, this::text, this::link);
					// New line between items
					newLine();
					item = item.getNext();
				} while (item != null);
			}

			private void text(Text text) {
				addText(text.getLiteral(), null);
			}

			private void link(Link link) {
				String text = link.getTitle();
				if (text == null) {
					StringBuilder sb = new StringBuilder();
					parse(link, t -> sb.append(t.getLiteral()), null);
					text = sb.toString();
				}
				addLink(text, link.getDestination());
			}

			private void parse(Node node, Consumer<Text> textHandler, Consumer<Link> linkHandler) {
				if (node instanceof Text) {
					if (textHandler != null) textHandler.accept((Text) node);
				} else if (linkHandler != null && node instanceof Link) {
					linkHandler.accept((Link) node);
				} else {
					Node child = node.getFirstChild();
					do {
						parse(child, textHandler, linkHandler);
						child = child.getNext();
					} while (child != null);
				}
			}

			private void addLink(String text, String url) {
				Hyperlink link = new Hyperlink(text);
				link.getStyleClass().add("a");
				link.setOnAction(e -> {
					try {
						UiUtil.showDocument(new URL(url).toURI());
					} catch (Exception ex) {
						Log.error("Could not open URL: " + url);
					}
				});
				flow.getChildren().add(link);
			}

			private void addText(String text, String style) {
				javafx.scene.text.Text t = new javafx.scene.text.Text(text);
				if (style != null)
					t.getStyleClass().add(style);
				flow.getChildren().add(t);
			}

			private void newLine() {
				javafx.scene.text.Text t = new javafx.scene.text.Text("\n");
				flow.getChildren().add(t);
			}
		});
		BorderPane pane = new BorderPane(flow);
		pane.getStyleClass().add("content");
		pane.getStyleClass().add("update-notes");
		return pane;
	}

	private Label getSubMessage() {
		Label label = new Label(translate("update.consent"));
		label.getStyleClass().add("faint");
		return label;
	}

	private VBox getInfo() {
		VBox box = new VBox();
		int sizeInMg = SelfUpdater.getLatestArtifactSize() / 1000000;
		box.getChildren().addAll(new Label("Size: " + sizeInMg + " MB"),
				new Label("Date: " + SelfUpdater.getLatestVersionDate().toString()));
		return box;
	}

	private Button getButton() {
		Button button = new ActionButton(translate("update.download"), this::update);
		button.setMaxHeight(Double.MAX_VALUE);
		button.setMaxWidth(Double.MAX_VALUE);
		return button;
	}

	private void update() {
		try {
			SelfUpdater.updateRecaf();
			controller.exit();
		} catch (IOException ex) {
			Log.error(ex, "Failed to start update process");
			ExceptionAlert.show(ex, "Recaf failed to start the update process");
		}
	}
}
