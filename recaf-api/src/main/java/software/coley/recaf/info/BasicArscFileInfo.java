package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.builder.ArscFileInfoBuilder;
import software.coley.recaf.util.android.AndroidRes;

/**
 * Basic implementation of ARSC file info.
 *
 * @author Matt Coley
 */
public class BasicArscFileInfo extends BasicAndroidChunkFileInfo implements ArscFileInfo {
	private AndroidRes res;

	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public BasicArscFileInfo(ArscFileInfoBuilder builder) {
		super(builder);
	}

	@Nonnull
	@Override
	public AndroidRes getResourceInfo() {
		if (res == null)
			res = AndroidRes.fromArsc(this);
		return res;
	}
}
