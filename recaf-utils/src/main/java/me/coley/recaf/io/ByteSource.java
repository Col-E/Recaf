package me.coley.recaf.io;

import java.io.IOException;
import java.io.InputStream;

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
	 * Streams this source.
	 *
	 * @return Stream for this source.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	InputStream openStream() throws IOException;
}
