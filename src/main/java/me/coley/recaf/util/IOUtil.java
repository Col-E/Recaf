package me.coley.recaf.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Faster I/O utils
 *
 * @author xxDark
 */
public final class IOUtil {
	/**
	 * Indicates that we can read as many bytes
	 * as we want.
	 */
	public static final int ANY = Integer.MIN_VALUE;

	private IOUtil() { }

	/**
	 * Transfers data from input to output stream.
	 *
	 * @param in     an input stream
	 * @param out    an output stream
	 * @param buffer data buffer
	 * @param max    maximum amount of bytes to transfer
	 * @return amount of bytes read
	 * @throws IOException if any I/O error occurs
	 */
	public static int transfer(InputStream in, OutputStream out, byte[] buffer, int max) throws IOException {
		int transferred = 0;
		int r;
		while ((max == ANY || max > 0) && (r = in.read(buffer, 0, buffer.length)) != -1) {
			transferred += r;
			out.write(buffer, 0, r);
			if (max != ANY) {
				max -= r;
			}
		}
		return transferred;
	}

	/**
	 * Transfers data from input to output stream.
	 * No limits.
	 *
	 * @param in     an input stream
	 * @param out    an output stream
	 * @param buffer data buffer
	 * @return amount of bytes read
	 * @throws IOException if any I/O error occurs
	 */
	public static int transfer(InputStream in, OutputStream out, byte[] buffer) throws IOException {
		return transfer(in, out, buffer, ANY);
	}

	/**
	 * Reads data from input stream to byte array.
	 *
	 * @param in     an input stream
	 * @param out    an output stream
	 * @param buffer data buffer
	 * @param max    maximum amount of bytes to transfer
	 * @return array of bytes
	 * @throws IOException if any I/O error occurs
	 */
	public static byte[] toByteArray(InputStream in, ByteArrayOutputStream out, byte[] buffer, int max)
			throws IOException {
		transfer(in, out, buffer, max);
		return out.toByteArray();
	}

	/**
	 * Reads data from input stream to byte array.
	 * No limits.
	 *
	 * @param in     an input stream
	 * @param out    an output stream
	 * @param length data buffer length
	 * @param max    maximum amount of bytes to transfer
	 * @return array of bytes
	 * @throws IOException if any I/O error occurs
	 */
	public static byte[] toByteArray(InputStream in, ByteArrayOutputStream out, int length, int max) throws IOException {
		return toByteArray(in, out, new byte[length], max);
	}

	/**
	 * Reads data from input stream to byte array.
	 * No limits.
	 *
	 * @param in     an input stream
	 * @param out    an output stream
	 * @param buffer data buffer
	 * @return array of bytes
	 * @throws IOException if any I/O error occurs
	 */
	public static byte[] toByteArray(InputStream in, ByteArrayOutputStream out, byte[] buffer) throws IOException {
		return toByteArray(in, out, buffer, ANY);
	}

	/**
	 * Reads data from input stream to byte array.
	 * No limits.
	 *
	 * @param in     an input stream
	 * @param out    an output stream
	 * @param length data buffer length
	 * @return array of bytes
	 * @throws IOException if any I/O error occurs
	 */
	public static byte[] toByteArray(InputStream in, ByteArrayOutputStream out, int length) throws IOException {
		return toByteArray(in, out, new byte[length]);
	}

	/**
	 * Reads data from input stream to byte array.
	 * No limits.
	 *
	 * @param in  an input stream
	 * @param out an output stream
	 * @return array of bytes
	 * @throws IOException if any I/O error occurs
	 */
	public static byte[] toByteArray(InputStream in, ByteArrayOutputStream out) throws IOException {
		return toByteArray(in, out, new byte[4096]);
	}

	/**
	 * Reads data from input stream to byte array.
	 * No limits.
	 *
	 * @param in an input stream
	 * @return array of bytes
	 * @throws IOException if any I/O error occurs
	 */
	public static byte[] toByteArray(InputStream in) throws IOException {
		return toByteArray(in, new ByteArrayOutputStream(in.available()));
	}

	/**
	 * Reads data from input stream to byte array.
	 * No limits.
	 *
	 * @param in an input stream
	 * @return array of bytes
	 * @throws IOException if any I/O error occurs
	 */
	public static byte[] toByteArray(InputStream in, byte[] buffer) throws IOException {
		return toByteArray(in, new ByteArrayOutputStream(in.available()), buffer);
	}

	/**
	 * Reads data from input stream to byte array.
	 * No limits.
	 *
	 * @param in     an input stream
	 * @param length data buffer length
	 * @return array of bytes
	 * @throws IOException if any I/O error occurs
	 */
	public static byte[] toByteArray(InputStream in, int length) throws IOException {
		return toByteArray(in, new ByteArrayOutputStream(in.available()), length);
	}
}
