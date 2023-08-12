package software.coley.recaf.info.builder;

import software.coley.recaf.info.BasicVideoFileInfo;
import software.coley.recaf.info.VideoFileInfo;

/**
 * Builder for {@link VideoFileInfo}.
 *
 * @author Matt Coley
 */
public class VideoFileInfoBuilder extends FileInfoBuilder<VideoFileInfoBuilder> {
	public VideoFileInfoBuilder() {
		// empty
	}

	public VideoFileInfoBuilder(VideoFileInfo videoFileInfo) {
		super(videoFileInfo);
	}

	public VideoFileInfoBuilder(FileInfoBuilder<?> other) {
		super(other);
	}

	@Override
	public BasicVideoFileInfo build() {
		return new BasicVideoFileInfo(this);
	}
}
