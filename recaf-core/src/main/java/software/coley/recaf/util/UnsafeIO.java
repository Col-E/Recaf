package software.coley.recaf.util;

import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;

/**
 * Unsafe IO utilities.
 *
 * @author xDark
 */
public final class UnsafeIO {
	private static final Logger logger = Logging.get(UnsafeIO.class);
	private static final MethodHandle BR_SET_BUFFER;
	private static final MethodHandle BW_SET_BUFFER;
	private static final MethodHandle BW_SET_NCHARS;

	/**
	 * Deny all constructions.
	 */
	private UnsafeIO() {
	}

	/**
	 * Updates underlying buffer of a given {@link BufferedReader}.
	 *
	 * @param reader
	 * 		Reader to update.
	 * @param buffer
	 * 		Buffer to set.
	 */
	public static void setReaderBuffer(BufferedReader reader, char[] buffer) {
		try {
			BR_SET_BUFFER.invokeExact(reader, buffer);
		} catch (Throwable t) {
			logger.error("Failed to set buffer for reader", t);
			throw new AssertionError(t);
		}
	}

	/**
	 * Updates underlying buffer of a given {@link BufferedWriter}.
	 *
	 * @param writer
	 * 		Writer to update.
	 * @param buffer
	 * 		Buffer to set.
	 */
	public static void setWriterBuffer(BufferedWriter writer, char[] buffer) {
		try {
			BW_SET_BUFFER.invokeExact(writer, buffer);
			BW_SET_NCHARS.invokeExact(writer, buffer.length);
		} catch (Throwable t) {
			logger.error("Failed to set buffer for writer", t);
			throw new AssertionError(t);
		}
	}

	static {
		try {
			Lookup lookup = ReflectUtil.lookup();
			BR_SET_BUFFER = lookup.findSetter(BufferedReader.class, "cb", char[].class);
			BW_SET_BUFFER = lookup.findSetter(BufferedWriter.class, "cb", char[].class);
			BW_SET_NCHARS = lookup.findSetter(BufferedWriter.class, "nChars", Integer.TYPE);
		} catch (NoSuchFieldException | IllegalAccessException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}
}
