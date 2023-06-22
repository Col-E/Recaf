package software.coley.recaf.info;

import software.coley.recaf.info.builder.ZipFileInfoBuilder;

/**
 * Basic implementation of an Android APK file info.
 *
 * @author Matt Coley
 */
public class BasicApkFileInfo extends BasicZipFileInfo implements ApkFileInfo {
	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public BasicApkFileInfo(ZipFileInfoBuilder builder) {
		super(builder);
	}
}
