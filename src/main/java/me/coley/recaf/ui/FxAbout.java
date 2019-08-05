package me.coley.recaf.ui;

import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import me.coley.recaf.Recaf;
import me.coley.recaf.util.*;

/**
 * Window for displaying java specs, recaf version and contributors.
 *
 * @author Matt
 */
public class FxAbout extends Stage {
	private final static FxAbout INSTANCE = new FxAbout();

	private FxAbout() {
		setTitle(Lang.get("ui.menubar.help.about"));
		getIcons().add(Icons.LOGO);
		setResizable(false);
		addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent e) -> {
			if(KeyCode.ESCAPE == e.getCode()) {
				hide();
			}
		});
		BorderPane bp = new BorderPane();
		bp.getStyleClass().add("about-panel");
		ImageView img = new ImageView(Icons.ABOUT_BAR);
		img.fitWidthProperty().bind(widthProperty());

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setAlignment(Pos.CENTER);
		grid.add(new BoldLabel("Recaf version:"), 0, 0);
		grid.add(new Label(Recaf.VERSION), 1, 0);
		grid.add(new BoldLabel("Java version:"), 0, 1);
		grid.add(new Label(System.getProperty("java.version")), 1, 1);
		grid.add(new BoldLabel("JVM:"), 0, 2);
		grid.add(new Label(System.getProperty("java.vm.name")), 1, 2);
		grid.add(new BoldLabel("Contributors:"), 0, 3);
		grid.add(new Label(String.join("\n", new String[]{
				"0x90 (AxDSan)",
				"Andy Li (andylizi)",
				"Charles Daniels",
				"lollo",
				"Matt Coley (Col-E)",
				"Michael Savich",
				"TimmyOVO",
				"xxDark"
		})), 1, 3);
		for(Node n : grid.getChildrenUnmodifiable()) {
			GridPane.setHalignment(n, HPos.LEFT);
			GridPane.setValignment(n, VPos.TOP);
		}
		bp.setTop(img);
		bp.setCenter(grid);
		setScene(JavaFX.scene(bp, 490, 480));
	}

	/**
	 * Display about window.
	 */
	public static void open() {
		if(INSTANCE.isShowing()) {
			INSTANCE.toFront();
		} else {
			INSTANCE.show();
		}
	}

	public static class BoldLabel extends Label {
		public BoldLabel(String text) {
			super(text);
			setStyle("-fx-font-weight: bold");
			setAlignment(Pos.TOP_LEFT);
		}
	}
}
