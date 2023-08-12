package software.coley.recaf.info;

import software.coley.recaf.info.builder.AudioFileInfoBuilder;

/**
 * Basic implementation of an audio file info.
 *
 * @author Matt Coley
 */
public class BasicAudioFileInfo extends BasicFileInfo implements AudioFileInfo {
	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public BasicAudioFileInfo(AudioFileInfoBuilder builder) {
		super(builder);
	}
}
