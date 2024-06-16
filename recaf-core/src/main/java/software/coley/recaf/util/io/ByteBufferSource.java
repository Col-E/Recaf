package software.coley.recaf.util.io;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

/**
 * Buffer byte source.
 *
 * @author xDark
 */
public final class ByteBufferSource implements ByteSource {
	private final ByteSource source;

	/**
	 * @param buffer
	 * 		Buffer.
	 */
	public ByteBufferSource(ByteBuffer buffer) {
		source = ByteSources.forMemorySegment(MemorySegment.ofBuffer(buffer));
	}

	@Nonnull
	@Override
	public byte[] readAll() throws IOException {
		return source.readAll();
	}

	@Nonnull
	@Override
	public byte[] peek(int count) throws IOException {
		return source.peek(count);
	}

	@Nonnull
	@Override
	public InputStream openStream() throws IOException {
		return source.openStream();
	}

	@Nonnull
	@Override
	public MemorySegment mmap() throws IOException {
		return source.mmap();
	}
}
