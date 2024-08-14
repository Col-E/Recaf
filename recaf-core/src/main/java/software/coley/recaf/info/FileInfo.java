package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.builder.FileInfoBuilder;

/**
 * Outline of a file.
 *
 * @author Matt Coley
 */
public interface FileInfo extends Info, Named {
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
	 * @return File extension of {@link #getName() the file name}, if any exists.
	 */
	@Nullable
	default String getFileExtension() {
		String fileName = getName();
		int i = fileName.indexOf('.') + 1;
		if (i > 0)
			return fileName.toLowerCase().substring(i);
		return null;
	}

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
	 * @return Self cast to image file.
	 */
	@Nonnull
	default ImageFileInfo asImageFile() {
		throw new IllegalStateException("Non-image file cannot be cast to image file");
	}

	/**
	 * @return Self cast to audio file.
	 */
	@Nonnull
	default AudioFileInfo asAudioFile() {
		throw new IllegalStateException("Non-audio file cannot be cast to audio file");
	}

	/**
	 * @return Self cast to video file.
	 */
	@Nonnull
	default VideoFileInfo asVideoFile() {
		throw new IllegalStateException("Non-video file cannot be cast to video file");
	}

	/**
	 * @return Self cast to video file.
	 */
	@Nonnull
	default NativeLibraryFileInfo asNativeLibraryFile() {
		throw new IllegalStateException("Non-native-library file cannot be cast to native-library file");
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
	 * @return {@code true} if self is a text file.
	 */
	default boolean isTextFile() {
		return false;
	}

	/**
	 * @return {@code true} if self is an image file.
	 */
	default boolean isImageFile() {
		return false;
	}

	/**
	 * @return {@code true} if self is an audio file.
	 */
	default boolean isAudioFile() {
		return false;
	}

	/**
	 * @return {@code true} if self is a video file.
	 */
	default boolean isVideoFile() {
		return false;
	}

	/**
	 * @return {@code true} if self is a native-library file.
	 */
	default boolean isNativeLibraryFile() {
		return false;
	}

	/**
	 * @return {@code true} if self is a zip file.
	 */
	default boolean isZipFile() {
		return false;
	}
}
