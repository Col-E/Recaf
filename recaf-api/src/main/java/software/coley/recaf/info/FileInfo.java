package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.builder.FileInfoBuilder;

/**
 * Outline of a file.
 *
 * @author Matt Coley
 */
public interface FileInfo extends Info {
	/**
	 * @return New builder wrapping this file information.
	 */
	@Nonnull
	default FileInfoBuilder<?> toFileBuilder() {
		return FileInfoBuilder.forFile(this);
	}

	/**
	 * @return Raw bytes of file content.
	 */
	@Nonnull
	byte[] getRawContent();

	/**
	 * @return Directory the file resides in.
	 * May be {@code null} for files in the root directory.
	 */
	@Nullable
	default String getDirectoryName() {
		String fileName = getName();
		int directoryIndex = fileName.lastIndexOf('/');
		if (directoryIndex <= 0) return null;
		return fileName.substring(0, directoryIndex);
	}

	@Nonnull
	@Override
	default ClassInfo asClass() {
		throw new IllegalStateException("File cannot be cast to generic class");
	}

	@Nonnull
	@Override
	default FileInfo asFile() {
		return this;
	}

	/**
	 * @return Self cast to text file.
	 */
	@Nonnull
	default TextFileInfo asTextFile() {
		throw new IllegalStateException("Non-text file cannot be cast to text file");
	}

	/**
	 * @return Self cast to zip file.
	 */
	@Nonnull
	default ZipFileInfo asZipFile() {
		throw new IllegalStateException("Non-zip file cannot be cast to zip file");
	}

	@Override
	default boolean isClass() {
		return false;
	}

	@Override
	default boolean isFile() {
		return true;
	}

	/**
	 * @return {@code true} if self is a zip file.
	 */
	default boolean isTextFile() {
		return false;
	}

	/**
	 * @return {@code true} if self is a zip file.
	 */
	default boolean isZipFile() {
		return false;
	}
}
