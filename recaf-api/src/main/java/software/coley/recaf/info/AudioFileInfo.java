package software.coley.recaf.info;

import jakarta.annotation.Nonnull;

/**
 * Outline of an audio file.
 *
 * @author Matt Coley
 */
public interface AudioFileInfo extends FileInfo {
	@Nonnull
	@Override
	default AudioFileInfo asAudioFile() {
		return this;
	}

	@Override
	default boolean isAudioFile() {
		return true;
	}
}
