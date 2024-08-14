package software.coley.recaf.info.builder;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.AudioFileInfo;
import software.coley.recaf.info.BasicAudioFileInfo;
import software.coley.recaf.info.BasicNativeLibraryFileInfo;
import software.coley.recaf.info.NativeLibraryFileInfo;

/**
 * Builder for {@link NativeLibraryFileInfo}.
 *
 * @author Matt Coley
 */
public class NativeLibraryFileInfoBuilder extends FileInfoBuilder<NativeLibraryFileInfoBuilder> {
	public NativeLibraryFileInfoBuilder() {
		// empty
	}

	public NativeLibraryFileInfoBuilder(NativeLibraryFileInfo libraryFileInfo) {
		super(libraryFileInfo);
	}

	public NativeLibraryFileInfoBuilder(FileInfoBuilder<?> other) {
		super(other);
	}

	@Nonnull
	@Override
	public BasicNativeLibraryFileInfo build() {
		return new BasicNativeLibraryFileInfo(this);
	}
}
