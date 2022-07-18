package me.coley.recaf.ui.control.media;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Bloom;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.behavior.FileRepresentation;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.media.AudioPlayer;
import me.coley.recaf.ui.media.CombinedPlayer;
import me.coley.recaf.ui.media.FxPlayer;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * A pane for displaying and playing audio files.
 *
 * @author Matt Coley
 */
public class AudioPane extends BorderPane implements FileRepresentation, Cleanable {
	private static final Logger logger = Logging.get(AudioPane.class);
	private final AudioPlayer player = new CombinedPlayer(List.of(new FxPlayer()));
	private final Canvas canvas = new Canvas();
	private FileInfo fileInfo;

	/**
	 * Setup the pane.
	 */
	public AudioPane() {
		canvas.widthProperty().bind(widthProperty().subtract(10));
		canvas.heightProperty().bind(heightProperty().subtract(10));
		setCenter(canvas);
		// TODO: Play/pause/stop
		//  - volume
		//  - equalizer?
		// Faint bloom effect
		Bloom bloom = new Bloom(0.6);
		player.setSpectrumListener(event -> {
			// Clear the screen
			GraphicsContext g = canvas.getGraphicsContext2D();
			double width = canvas.getWidth();
			double height = canvas.getHeight();
			g.setFill(Color.BLACK);
			g.fillRect(0, 0, width, height);
			// Prepare for coloring spikes
			g.setFill(LinearGradient.valueOf("linear-gradient(to bottom, blue, white)"));
			// Draw each magnitude as a spike shape
			float[] magnitudes = event.getMagnitudes();
			int max = magnitudes.length;
			for (int i = 1; i < max - 1; i++) {
				double percent = (double) i / max;
				double nextPercent = (double) (i + 1) / max;
				double x = percent * width;
				double nx = nextPercent * width;
				float magnitude = -magnitudes[i];
				double magnitudeMax = 60;
				double magnitudePercent = magnitude / magnitudeMax;
				if (magnitudePercent < 0)
					magnitudePercent = 0;
				else if (magnitudePercent > 1)
					magnitudePercent = 1;
				double magnitudeHeight = magnitudePercent * height;
				// Draw spike for the sample
				g.beginPath();
				g.moveTo(x, height);
				g.lineTo((x + nx) / 2, magnitudeHeight);
				g.lineTo(nx, height);
				g.closePath();
				g.fill();
			}
			// Makes the spike slightly softer looking
			g.applyEffect(bloom);
		});
	}

	private void onLoadFailure(IOException ex) {
		logger.warn("Could not load audio from '{}'", fileInfo.getName(), ex);
		FxThreadUtil.delayedRun(100, () -> {
			GraphicsContext g = canvas.getGraphicsContext2D();
			double w = canvas.getWidth();
			double h = canvas.getHeight();
			g.setFill(Color.BLACK);
			g.fillRect(0, 0, w, h);
			g.setFill(Color.WHITE);
			g.fillText(ex.getMessage(), 10, h / 2);
		});
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
	public void onUpdate(FileInfo newValue) {
		fileInfo = newValue;
		try {
			player.stop();
			player.load(newValue.getName());
			player.play();
		} catch (IOException ex) {
			onLoadFailure(ex);
		}
	}

	@Override
	public void cleanup() {
		player.reset();
	}
}
