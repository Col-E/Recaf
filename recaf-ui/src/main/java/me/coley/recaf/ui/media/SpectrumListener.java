package me.coley.recaf.ui.media;

/**
 * Simple spectrum data listener.
 *
 * @author Matt Coley
 */
public interface SpectrumListener {
	/**
	 * @param event Spectrum data.
	 */
	void onSpectrum(SpectrumEvent event);
}
