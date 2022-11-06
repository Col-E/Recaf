package me.coley.recaf.ui.control.media;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.ui.control.ResizableCanvas;
import me.coley.recaf.ui.media.CombinedPlayer;
import me.coley.recaf.ui.media.FxPlayer;
import me.coley.recaf.ui.media.Player;
import me.coley.recaf.util.threading.FxThreadUtil;

import java.io.IOException;
import java.util.Collections;

/**
 * A pane for displaying and playing audio files.
 *
 * @author Matt Coley
 */
public class AudioPane extends MediaPane {
	private final Canvas canvas = new ResizableCanvas();
	private final Player player = new CombinedPlayer(Collections.singletonList(new FxPlayer()));

	/**
	 * Setup the pane.
	 */
	public AudioPane() {
		setCenter(canvas);
		// Makes the spike slightly softer looking
		// The combination of the faint blur and bloom prevent the 'creeping glow' that occurs with just the bloom
		// when used on lower resolutions.
		GaussianBlur blur = new GaussianBlur(1);
		Bloom bloom = new Bloom();
		bloom.setThreshold(0.6);
		blur.setInput(bloom);
		player.setSpectrumListener(event -> {
			// Clear the screen
			GraphicsContext g = canvas.getGraphicsContext2D();
			double width = canvas.getWidth() + 2;
			double height = canvas.getHeight() + 2;
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
			g.applyEffect(blur);
			drawTimeText(g);
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

	private void initialDraw() {
		FxThreadUtil.delayedRun(100, () -> {
			GraphicsContext g = canvas.getGraphicsContext2D();
			double w = canvas.getWidth();
			double h = canvas.getHeight();
			g.setFill(Color.BLACK);
			g.fillRect(0, 0, w, h);
			drawTimeText(g);
		});
	}

	private void drawTimeText(GraphicsContext g) {
		double w = canvas.getWidth();
		int max = (int) player.getMaxSeconds();
		int cur = (int) player.getCurrentSeconds();
		g.setFill(Color.WHITE);
		g.fillText(formatTime(cur) + " / " + formatTime(max), w - 80, 24);
	}

	private static String formatTime(int time) {
		int minutes = time / 60;
		int seconds = time - minutes * 60;
		String formattedTime = "";
		if (minutes < 10)
			formattedTime += "0";
		formattedTime += minutes + ":";
		if (seconds < 10)
			formattedTime += "0";
		formattedTime += seconds;
		return formattedTime;
	}

	@Override
	public void onUpdate(FileInfo newValue) {
		fileInfo = newValue;
		try {
			player.stop();
			player.load(newValue.getName());
			initialDraw();
		} catch (IOException ex) {
			onLoadFailure(ex);
		}
	}

	@Override
	protected Player getPlayer() {
		return player;
	}
}
