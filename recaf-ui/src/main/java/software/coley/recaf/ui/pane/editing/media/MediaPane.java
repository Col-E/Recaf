package software.coley.recaf.ui.pane.editing.media;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.services.navigation.FileNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.media.Player;
import software.coley.recaf.util.FxThreadUtil;

import java.util.Collection;
import java.util.Collections;

/**
 * Common base for media players.
 *
 * @author Matt Coley
 * @see VideoPane
 * @see AudioPane
 */
public abstract class MediaPane extends BorderPane implements FileNavigable, UpdatableNavigable {
	protected static final Logger logger = Logging.get(MediaPane.class);
	protected FilePathNode path;

	protected abstract Player getPlayer();

	protected MediaPane() {
		setBottom(interactionBar());
		setStyle("-fx-background-color: black");
	}

	protected Region interactionBar() {
		HBox buttonBox = new HBox();
		Button btnPlay = new Button();
		Button btnPause = new Button();
		Button btnStop = new Button();
		btnPause.setDisable(true);
		btnStop.setDisable(true);
		btnPlay.setGraphic(new FontIconView(CarbonIcons.PLAY_FILLED_ALT));
		btnPause.setGraphic(new FontIconView(CarbonIcons.PAUSE_FILLED));
		btnStop.setGraphic(new FontIconView(CarbonIcons.STOP_FILLED_ALT));
		btnPlay.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
		btnPause.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
		btnStop.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
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
		buttonBox.getChildren().addAll(btnPlay, btnPause, btnStop);
		buttonBox.setAlignment(Pos.CENTER);
		buttonBox.setPadding(new Insets(1));
		Slider slider = new Slider();
		FxThreadUtil.delayedRun(250, () -> {
			Player player = getPlayer();
			double maxSeconds = player.getMaxSeconds() * 1000;
			slider.setMin(0);
			slider.setMax(maxSeconds);
			slider.setValue(0);

			// While scrubbing, seek the media playback
			slider.valueProperty().addListener((_, _, seek) -> {
				if (slider.isPressed())
					player.seek(seek.doubleValue());
			});

			// While not scrubbing, update the slider position
			player.addPlaybackListener(() -> {
				if (!slider.isPressed())
					slider.setValue(player.getCurrentSeconds() * 1000);
			});
		});
		HBox wrapper = new HBox(buttonBox, new Spacer(), slider);
		HBox.setHgrow(slider, Priority.ALWAYS);
		wrapper.setAlignment(Pos.CENTER);
		return wrapper;
	}

	@Nonnull
	@Override
	public FilePathNode getPath() {
		return path;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		getPlayer().dispose();
	}
}
