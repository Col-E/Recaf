package me.coley.recaf.ui.control.media;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.behavior.FileRepresentation;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.media.Player;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

/**
 * Common base for media players.
 *
 * @author Matt Coley
 * @see VideoPane
 * @see AudioPane
 */
public abstract class MediaPane extends BorderPane implements FileRepresentation, Cleanable {
	protected static final Logger logger = Logging.get(MediaPane.class);
	protected FileInfo fileInfo;

	protected abstract Player getPlayer();

	protected MediaPane() {
		setBottom(interactionBar());
		setStyle("-fx-background-color: black");
	}

	protected Region interactionBar() {
		HBox box = new HBox();
		Button btnPlay = new Button();
		Button btnPause = new Button();
		Button btnStop = new Button();
		btnPause.setDisable(true);
		btnStop.setDisable(true);
		btnPlay.setGraphic(Icons.getIconView(Icons.PLAY));
		btnPause.setGraphic(Icons.getIconView(Icons.PAUSE));
		btnStop.setGraphic(Icons.getIconView(Icons.STOP));
		btnPlay.getStyleClass().add("rounded-button-left");
		btnStop.getStyleClass().add("rounded-button-right");
		btnPlay.setOnAction(e -> {
			getPlayer().play();
			btnPlay.setDisable(true);
			btnPause.setDisable(false);
			btnStop.setDisable(false);
		});
		btnPause.setOnAction(e -> {
			getPlayer().pause();
			btnPlay.setDisable(false);
			btnPause.setDisable(true);
			btnStop.setDisable(false);
		});
		btnStop.setOnAction(e -> {
			getPlayer().stop();
			btnPlay.setDisable(false);
			btnPause.setDisable(true);
			btnStop.setDisable(true);
		});
		box.getChildren().addAll(btnPlay, btnPause, btnStop);
		box.setAlignment(Pos.CENTER);
		box.setPadding(new Insets(1));
		return box;
	}

	@Override
	public FileInfo getCurrentFileInfo() {
		return fileInfo;
	}

	@Override
	public SaveResult save() {
		return SaveResult.IGNORED;
	}

	@Override
	public boolean supportsEditing() {
		return false;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	@Override
	public void cleanup() {
		getPlayer().reset();
	}
}
