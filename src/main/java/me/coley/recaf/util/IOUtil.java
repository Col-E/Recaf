package me.coley.recaf.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Faster I/O utils
 *
 * @author xxDark
 */
public final class IOUtil {
	private IOUtil() { }

	/**
	 * Transfers data from input to output stream.
	 *
	 * @param in     an input stream
	 * @param out    an output stream
	 * @param buffer data buffer
	 * @return amount of bytes read
	 * @throws IOException if any I/O error occurs
	 */
	public static int transfer(InputStream in, OutputStream out, byte[] buffer) throws IOException {
		int transferred = 0;
		int r;
		while ((r = in.read(buffer, 0, buffer.length)) != -1) {
			transferred += r;
			out.write(buffer, 0, r);
		}
		return transferred;
	}

	/**
	 * Reads data from input stream to byte array.
	 *
	 * @param in     an input stream
	 * @param out    an output stream
	 * @param buffer data buffer
	 * @return array of bytes
	 * @throws IOException if any I/O error occurs
	 */
	public static byte[] toByteArray(InputStream in, ByteArrayOutputStream out, byte[] buffer) throws IOException {
		transfer(in, out, buffer);
		return out.toByteArray();
	}

	public static void close(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException ignored) {
				// No-op
			}
		}
	}
}
