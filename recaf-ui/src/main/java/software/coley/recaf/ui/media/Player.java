package software.coley.recaf.ui.media;

import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;

/**
 * A common base for media players sourced from different back-ends.
 *
 * @author Matt Coley
 * @see FxPlayer
 * @see CombinedPlayer
 */
public abstract class Player {
	private SpectrumListener spectrumListener;

	/**
	 * Play the track.
	 */
	public abstract void play();

	/**
	 * Pause the track.
	 */
	public abstract void pause();

	/**
	 * Seek to the given time (in millis).
	 *
	 * @param millis
	 * 		Time to seek to.
	 */
	public abstract void seek(double millis);

	/**
	 * Stop the track.
	 */
	public abstract void stop();

	/**
	 * Stop the track and clear references.
	 */
	public abstract void reset();

	/**
	 * Clean up resources.
	 */
	public abstract void dispose();

	/**
	 * Adds a playback listener.
	 *
	 * @param r
	 * 		Runnable to call when the playback state changes.
	 */
	public abstract void addPlaybackListener(Runnable r);

	/**
	 * @return Current spectrum listener.
	 */
	public SpectrumListener getSpectrumListener() {
		return spectrumListener;
	}

	/**
	 * @param listener
	 * 		New spectrum listener.
	 */
	public void setSpectrumListener(SpectrumListener listener) {
		this.spectrumListener = listener;
	}

	/**
	 * Initialize a new track.
	 *
	 * @param path
	 * 		File path name in the current {@link Workspace}.
	 *
	 * @throws IOException
	 * 		When the file cannot be loaded for playback.
	 */
	public abstract void load(String path) throws IOException;

	/**
	 * @return Length of the currently loaded media in seconds,
	 * or a negative value if the duration could not be determined.
	 */
	public double getMaxSeconds() {
		return -1;
	}

	/**
	 * @return Current offset in the currently loaded media in seconds,
	 * or a negative value if the current time could not be determined.
	 */
	public double getCurrentSeconds() {
		return -1;
	}
}
