package software.coley.recaf.info;

import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceFile;
import jakarta.annotation.Nonnull;
import software.coley.recaf.info.builder.ChunkFileInfoBuilder;

/**
 * Common implementation for {@link BasicBinaryXmlFileInfo} and {@link BasicArscFileInfo}.
 *
 * @author Matt Coley
 */
public class BasicAndroidChunkFileInfo extends BasicFileInfo implements AndroidChunkFileInfo {
	private BinaryResourceFile resourceFile;

	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public BasicAndroidChunkFileInfo(ChunkFileInfoBuilder<?> builder) {
		super(builder);
	}

	@Nonnull
	@Override
	public BinaryResourceFile getChunkModel() {
		if (resourceFile == null)
			resourceFile = new BinaryResourceFile(getRawContent());
		return resourceFile;
	}
}
