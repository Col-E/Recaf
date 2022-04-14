package me.coley.recaf.io;

import java.io.IOException;

/**
 * Lazily provides byte array form a source.
 *
 * @author xDark
 */
public interface ByteSource {

	/**
	 * @return All bytes.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	byte[] readAll() throws IOException;

	/**
	 * Peeks some amount of bytes
	 * from the source.
	 *
	 * @param count
	 * 		Amount of bytes to peek.
	 *
	 * @return Peeked byte array which size may be
	 * smaller than the requested {@code count}.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	byte[] peek(int count) throws IOException;

	/**
	 * Byte source that wraps existing byte array.
	 *
	 * @param bytes
	 * 		Source content.
	 * @param offset
	 * 		Content offset.
	 * @param length
	 * 		Content length.
	 *
	 * @return New byte source.
	 */
	static ByteSource just(byte[] bytes, int offset, int length) {
		return new ByteArraySource(bytes, offset, length);
	}

	/**
	 * Byte source that wraps existing byte array.
	 *
	 * @param bytes
	 * 		Source content.
	 *
	 * @return New byte source.
	 */
	static ByteSource just(byte[] bytes) {
		return new ByteArraySource(bytes, 0, bytes.length);
	}
}
