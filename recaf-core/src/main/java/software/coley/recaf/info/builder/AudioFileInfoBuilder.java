package software.coley.recaf.info.builder;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.BasicAudioFileInfo;
import software.coley.recaf.info.AudioFileInfo;

/**
 * Builder for {@link AudioFileInfo}.
 *
 * @author Matt Coley
 */
public class AudioFileInfoBuilder extends FileInfoBuilder<AudioFileInfoBuilder> {
	public AudioFileInfoBuilder() {
		// empty
	}

	public AudioFileInfoBuilder(AudioFileInfo audioFileInfo) {
		super(audioFileInfo);
	}

	public AudioFileInfoBuilder(FileInfoBuilder<?> other) {
		super(other);
	}

	@Nonnull
	@Override
	public BasicAudioFileInfo build() {
		return new BasicAudioFileInfo(this);
	}
}
