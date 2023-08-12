package software.coley.recaf.ui.pane.editing.media;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.Label;
import javafx.scene.media.MediaView;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.ui.media.FxPlayer;
import software.coley.recaf.ui.media.Player;
import software.coley.recaf.util.FxThreadUtil;

import java.io.IOException;

/**
 * A pane for displaying and playing video files.
 *
 * @author Matt Coley
 */
@Dependent
public class VideoPane extends MediaPane {
	private final FxPlayer player = new FxPlayer();

	@Inject
	public VideoPane() {
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		if (path instanceof FilePathNode filePath) {
			this.path = filePath;
			FileInfo fileInfo = filePath.getValue();
			try {
				player.stop();
				player.load(fileInfo.getName());
				MediaView view = new MediaView(player.getPlayer());
				setCenter(view);
				view.setPreserveRatio(true);
				view.fitHeightProperty().bind(heightProperty().subtract(40));
			} catch (IOException ex) {
				onLoadFailure(fileInfo, ex);
			}
		}
	}

	private void onLoadFailure(@Nonnull FileInfo fileInfo, @Nonnull IOException ex) {
		logger.warn("Could not load video from '{}'", fileInfo.getName(), ex);
		FxThreadUtil.delayedRun(100, () -> {
			setCenter(new Label(ex.getMessage()));
		});
	}

	@Override
	protected Player getPlayer() {
		return player;
	}
}
