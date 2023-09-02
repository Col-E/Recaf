package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import software.coley.android.xml.AndroidResourceProvider;
import software.coley.android.xml.NoopAndroidResourceProvider;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.builder.ArscFileInfoBuilder;
import software.coley.recaf.util.android.AndroidRes;

/**
 * Basic implementation of ARSC file info.
 *
 * @author Matt Coley
 */
public class BasicArscFileInfo extends BasicAndroidChunkFileInfo implements ArscFileInfo {
	private static final Logger logger = Logging.get(BasicArscFileInfo.class);
	private AndroidResourceProvider res;

	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public BasicArscFileInfo(ArscFileInfoBuilder builder) {
		super(builder);
	}

	@Nonnull
	@Override
	public AndroidResourceProvider getResourceInfo() {
		if (res == null)
			try {
				res = AndroidRes.fromArsc(getChunkModel());
			} catch (Throwable t) {
				logger.error("Failed to decode '{}', will use an empty model instead", getName(), t);
				res = NoopAndroidResourceProvider.INSTANCE;
			}
		return res;
	}
}
