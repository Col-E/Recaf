package software.coley.recaf.info;

import jakarta.annotation.Nonnull;

/**
 * Outline of a native library or application file.
 *
 * @author Matt Coley
 */
public interface NativeLibraryFileInfo extends FileInfo {
	@Nonnull
	@Override
	default NativeLibraryFileInfo asNativeLibraryFile() {
		return this;
	}

	@Override
	default boolean isNativeLibraryFile() {
		return true;
	}
}
