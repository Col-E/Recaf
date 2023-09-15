package software.coley.recaf.info;

import software.coley.recaf.info.builder.BinaryXmlFileInfoBuilder;

/**
 * Basic implementation of binary XML file info.
 *
 * @author Matt Coley
 */
public class BasicBinaryXmlFileInfo extends BasicAndroidChunkFileInfo implements BinaryXmlFileInfo {
	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public BasicBinaryXmlFileInfo(BinaryXmlFileInfoBuilder builder) {
		super(builder);
	}
}
