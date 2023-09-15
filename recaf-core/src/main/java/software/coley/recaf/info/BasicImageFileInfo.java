package software.coley.recaf.info;

import software.coley.recaf.info.builder.ImageFileInfoBuilder;

/**
 * Basic implementation of an image file info.
 *
 * @author Matt Coley
 */
public class BasicImageFileInfo extends BasicFileInfo implements ImageFileInfo {
	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public BasicImageFileInfo(ImageFileInfoBuilder builder) {
		super(builder);
	}
}
