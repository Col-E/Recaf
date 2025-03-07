package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import software.coley.android.xml.AndroidResourceProvider;

/**
 * Outline of a ARSC file, used by Android APK's.
 *
 * @author Matt Coley
 */
public interface ArscFileInfo extends AndroidChunkFileInfo {
	/**
	 * Standard name of ARSC resource file in APK files.
	 */
	String NAME = "resources.arsc";

	/**
	 * @return Resource information extracted from the ARSC file contents.
	 */
	@Nonnull
	AndroidResourceProvider getResourceInfo();
}
