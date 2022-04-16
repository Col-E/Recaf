package me.coley.recaf.io;

import java.util.Arrays;

/**
 * Immediate byte source.
 * 
 * @author xDark
 */
final class ByteArraySource implements ByteSource {
	
	private final byte[] bytes;
	private final int off;
	private final int len;

	ByteArraySource(byte[] bytes, int off, int len) {
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
}
