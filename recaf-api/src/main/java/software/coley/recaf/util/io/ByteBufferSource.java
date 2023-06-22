package software.coley.recaf.util.io;

import jakarta.annotation.Nonnull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Buffer byte source.
 *
 * @author xDark
 */
public final class ByteBufferSource implements ByteSource {
	private final ByteBuffer buffer;

	/**
	 * @param buffer
	 * 		Buffer.
	 */
	public ByteBufferSource(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Nonnull
	@Override
	public byte[] readAll() {
		ByteBuffer buffer = this.buffer;
		byte[] data = new byte[buffer.remaining()];
		buffer.slice().get(data);
		return data;
	}

	@Nonnull
	@Override
	public byte[] peek(int count) {
		ByteBuffer buffer = this.buffer;
		count = Math.min(count, buffer.remaining());
		byte[] buf = new byte[count];
		buffer.slice().get(buf);
		return buf;
	}

	@Nonnull
	@Override
	public InputStream openStream() {
		return new ByteArrayInputStream(readAll());
	}
}
