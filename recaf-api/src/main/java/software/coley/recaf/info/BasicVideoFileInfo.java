package software.coley.recaf.info;

import software.coley.recaf.info.builder.VideoFileInfoBuilder;

/**
 * Basic implementation of a video file info.
 *
 * @author Matt Coley
 */
public class BasicVideoFileInfo extends BasicFileInfo implements VideoFileInfo {
	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public BasicVideoFileInfo(VideoFileInfoBuilder builder) {
		super(builder);
	}
}
