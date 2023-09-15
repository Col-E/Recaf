package software.coley.recaf.info;

import jakarta.annotation.Nonnull;

/**
 * Outline of an image file.
 *
 * @author Matt Coley
 */
public interface ImageFileInfo extends FileInfo {
	@Nonnull
	@Override
	default ImageFileInfo asImageFile() {
		return this;
	}

	@Override
	default boolean isImageFile() {
		return true;
	}
}
