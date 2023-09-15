package software.coley.recaf.info;

import jakarta.annotation.Nonnull;

/**
 * Outline of a ZIP file container.
 *
 * @author Matt Coley
 * @see JarFileInfo
 * @see WarFileInfo
 * @see JModFileInfo
 * @see ApkFileInfo
 */
public interface ZipFileInfo extends FileInfo {
	@Nonnull
	@Override
	default ZipFileInfo asZipFile() {
		return this;
	}

	@Override
	default boolean isZipFile() {
		return true;
	}
}
