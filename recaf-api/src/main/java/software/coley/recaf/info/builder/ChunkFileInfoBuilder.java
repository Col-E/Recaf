package software.coley.recaf.info.builder;

import software.coley.recaf.info.AndroidChunkFileInfo;

/**
 * Common builder for {@link ArscFileInfoBuilder} and {@link BinaryXmlFileInfoBuilder}.
 *
 * @param <B>
 * 		Self type. Exists so implementations don't get stunted in their chaining.
 *
 * @author Matt Coley
 */
public abstract class ChunkFileInfoBuilder<B extends ChunkFileInfoBuilder<?>> extends FileInfoBuilder<B> {
	public ChunkFileInfoBuilder() {
		// empty
	}

	public ChunkFileInfoBuilder(AndroidChunkFileInfo chunkInfo) {
		super(chunkInfo);
	}
}
