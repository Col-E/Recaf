package me.coley.recaf.ui.media;

/**
 * Spectrum data.
 *
 * @author Matt Coley
 */
public class SpectrumEvent {
	private final float[] magnitudes;

	/**
	 * @param magnitudes
	 * 		Magnitudes of the spectrum
	 */
	public SpectrumEvent(float[] magnitudes) {
		this.magnitudes = magnitudes;
	}

	/**
	 * @return Magnitudes of the spectrum
	 */
	public float[] getMagnitudes() {
		return magnitudes;
	}
}
