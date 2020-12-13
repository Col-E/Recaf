package me.coley.recaf.ui.controls.pane;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.ActionButton;
import me.coley.recaf.ui.controls.ExceptionAlert;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.self.SelfUpdater;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;

import java.io.IOException;

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
		Node document = parser.parse(SelfUpdater.getLatestPatchnotes().replaceAll("\\(\\[.+\\)\\)", ""));
		document.accept(new AbstractVisitor() {
			@Override
			public void visit(Text text) {
				Node parent = getRoot(text);
				if (parent instanceof Heading) {
					// Skip the version H1 text
					if (((Heading) parent).getLevel() == 1)
						return;
					//
					addLine(text.getLiteral(), "h2");
				} else if (parent instanceof BulletList || parent instanceof OrderedList) {
					String msg = text.getLiteral();
					addLine(" ● " + msg, null);
				}
			}

			private void addLine(String text, String style) {
				javafx.scene.text.Text t = new javafx.scene.text.Text(text + "\n");
				if (style != null)
					t.getStyleClass().add(style);
				flow.getChildren().add(t);
			}

			private Node getRoot(Node node) {
				while(!(node.getParent() instanceof Document))
					node = node.getParent();
				return node;
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
		} catch(IOException ex) {
			Log.error(ex, "Failed to start update process");
			ExceptionAlert.show(ex, "Recaf failed to start the update process");
		}
	}
}
