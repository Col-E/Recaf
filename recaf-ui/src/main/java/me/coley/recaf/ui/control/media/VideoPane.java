package me.coley.recaf.ui.control.media;

import javafx.scene.media.MediaView;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.ui.media.FxPlayer;
import me.coley.recaf.ui.media.Player;

import java.io.IOException;

/**
 * A pane for displaying and playing video files.
 *
 * @author Matt Coley
 */
public class VideoPane extends MediaPane {
	private final FxPlayer player = new FxPlayer();

	@Override
	public void onUpdate(FileInfo newValue) {
		fileInfo = newValue;
		try {
			player.stop();
			player.load(newValue.getName());
			MediaView view = new MediaView(player.getPlayer());
			setCenter(view);
			view.setPreserveRatio(true);
			view.fitHeightProperty().bind(heightProperty().subtract(40));
		} catch (IOException ex) {
			logger.warn("Could not load video from '{}'", fileInfo.getName(), ex);
		}
	}

	@Override
	protected Player getPlayer() {
		return player;
	}
}
