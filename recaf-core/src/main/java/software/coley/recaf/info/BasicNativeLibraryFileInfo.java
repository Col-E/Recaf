package software.coley.recaf.info;

import software.coley.recaf.info.builder.NativeLibraryFileInfoBuilder;

/**
 * Basic implementation of a native-library file info.
 *
 * @author Matt Coley
 */
public class BasicNativeLibraryFileInfo extends BasicFileInfo implements NativeLibraryFileInfo {
	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public BasicNativeLibraryFileInfo(NativeLibraryFileInfoBuilder builder) {
		super(builder);
	}
}
