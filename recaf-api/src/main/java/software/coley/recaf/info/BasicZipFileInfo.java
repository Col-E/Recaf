package software.coley.recaf.info;

import software.coley.recaf.info.builder.ZipFileInfoBuilder;

/**
 * Basic implementation of ZIP file info.
 *
 * @author Matt Coley
 */
public class BasicZipFileInfo extends BasicFileInfo implements ZipFileInfo {
	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public BasicZipFileInfo(ZipFileInfoBuilder builder) {
		super(builder);
	}
}
