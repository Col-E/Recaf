package me.coley.recaf.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Immediate byte source.
 *
 * @author xDark
 */
public final class ByteArraySource implements ByteSource {
	private final byte[] bytes;
	private final int off;
	private final int len;

	/**
	 * @param bytes
	 * 		Input bytes to wrap.
	 */
	public ByteArraySource(byte[] bytes) {
		this(bytes, 0, bytes.length);
	}

	/**
	 * @param bytes
	 * 		Input bytes to wrap.
	 * @param off
	 * 		Start offset.
	 * @param len
	 * 		Length of content.
	 */
	public ByteArraySource(byte[] bytes, int off, int len) {
		this.bytes = bytes;
		this.off = off;
		this.len = len;
	}

	@Override
	public byte[] readAll() {
		return Arrays.copyOfRange(bytes, off, len);
	}

	@Override
	public byte[] peek(int count) {
		count = Math.min(count, len);
		return Arrays.copyOfRange(bytes, off, count);
	}

	@Override
	public InputStream openStream() throws IOException {
		return new ByteArrayInputStream(bytes, off, len);
	}
}
