package me.coley.recaf.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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

	@Override
	public byte[] readAll() throws IOException {
		ByteBuffer buffer = this.buffer;
		byte[] data = new byte[buffer.remaining()];
		buffer.slice().get(data);
		return data;
	}

	@Override
	public byte[] peek(int count) throws IOException {
		ByteBuffer buffer = this.buffer;
		count = Math.min(count, buffer.remaining());
		byte[] buf = new byte[count];
		buffer.slice().get(buf);
		return buf;
	}

	@Override
	public InputStream openStream() throws IOException {
		return new ByteArrayInputStream(readAll());
	}
}
