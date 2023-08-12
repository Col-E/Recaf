package software.coley.recaf.info;

import jakarta.annotation.Nonnull;

/**
 * Outline of a video file.
 *
 * @author Matt Coley
 */
public interface VideoFileInfo extends FileInfo {
	@Nonnull
	@Override
	default VideoFileInfo asVideoFile() {
		return this;
	}

	@Override
	default boolean isVideoFile() {
		return true;
	}
}
