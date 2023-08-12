package software.coley.recaf.info.builder;

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

	public AudioFileInfoBuilder(AudioFileInfo imageFileInfo) {
		super(imageFileInfo);
	}

	public AudioFileInfoBuilder(FileInfoBuilder<?> other) {
		super(other);
	}

	@Override
	public BasicAudioFileInfo build() {
		return new BasicAudioFileInfo(this);
	}
}
