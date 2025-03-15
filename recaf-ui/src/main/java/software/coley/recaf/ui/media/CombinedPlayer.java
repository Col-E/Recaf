package software.coley.recaf.ui.media;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.util.List;

/**
 * Media-player that wraps multiple other implementations for wider support.
 *
 * @author Matt Coley
 */
public class CombinedPlayer extends Player {
	private final List<Player> delegates;
	private Player currentPlayer;

	/**
	 * @param delegates
	 * 		Backing players.
	 */
	public CombinedPlayer(@Nonnull List<Player> delegates) {
		this.delegates = delegates;
	}

	@Override
	public void play() {
		if (currentPlayer != null)
			currentPlayer.play();
	}

	@Override
	public void pause() {
		if (currentPlayer != null)
			currentPlayer.pause();
	}

	@Override
	public void seek(double millis) {
		if (currentPlayer != null)
			currentPlayer.seek(millis);
	}

	@Override
	public void stop() {
		if (currentPlayer != null)
			currentPlayer.stop();
	}

	@Override
	public void reset() {
		if (currentPlayer != null)
			currentPlayer.reset();
	}

	@Override
	public void dispose() {
		if (currentPlayer != null)
			currentPlayer.dispose();
	}

	@Override
	public void addPlaybackListener(Runnable r) {
		if (currentPlayer != null)
			currentPlayer.addPlaybackListener(r);
	}

	@Override
	public double getMaxSeconds() {
		if (currentPlayer != null)
			return currentPlayer.getMaxSeconds();
		return super.getMaxSeconds();
	}

	@Override
	public double getCurrentSeconds() {
		if (currentPlayer != null)
			return currentPlayer.getCurrentSeconds();
		return super.getCurrentSeconds();
	}

	@Override
	public void setSpectrumListener(SpectrumListener listener) {
		super.setSpectrumListener(listener);
		// Also set listener for delegated players
		delegates.forEach(delegate -> delegate.setSpectrumListener(listener));
	}

	@Override
	public void load(String path) throws IOException {
		// Reset prior content
		stop();
		// Attempt to load content from delegates
		IOException lastError = null;
		for (Player player : delegates) {
			try {
				player.load(path);
				// Success, use ths player
				currentPlayer = player;
				return;
			} catch (IOException ex) {
				lastError = ex;
				// ignore to allow other delegated players to attempt loading
			}
		}
		throw new IOException("Failed to load audio file from path: " + path, lastError);
	}
}
