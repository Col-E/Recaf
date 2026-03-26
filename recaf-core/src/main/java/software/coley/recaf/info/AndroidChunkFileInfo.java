package software.coley.recaf.info;

import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceFile;
import com.google.devrel.gmscore.tools.apk.arsc.ChunkWithChunks;
import com.google.devrel.gmscore.tools.apk.arsc.StringPoolChunk;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.collect.Streams;

import java.util.stream.Stream;

/**
 * Outline of files that utilize the Android chunk format.
 *
 * @author Matt Coley
 * @see BinaryXmlFileInfo For {@code AndroidManifest.xml} contents.
 * @see ArscFileInfo For {@code resources.arsc} contents.
 */
public interface AndroidChunkFileInfo extends FileInfo {
	/**
	 * @return Model representation of the chunk file.
	 */
	@Nonnull
	BinaryResourceFile getChunkModel();

	/**
	 * @return String pool chunk if it exists, otherwise {@code null}.
	 */
	@Nullable
	default StringPoolChunk getStringPoolChunk() {
		// The string pool chunk should the first chunk in the file.
		// But just in case, we'll search through all chunks and their children to find it.
		return Streams.recurse(getChunkModel().getChunks().getFirst(), chunk ->
						(chunk instanceof ChunkWithChunks chunkWithChunks) ? chunkWithChunks.getChunks().values().stream() : Stream.empty())
				.filter(c -> c instanceof StringPoolChunk)
				.map(c -> (StringPoolChunk) c)
				.findFirst().orElse(null);
	}
}