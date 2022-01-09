package me.coley.recaf.util;

/**
 * Some useful {@link ThreadLocal}s.
 *
 * @author xDark
 */
public final class ThreadLocals {
	private static final ThreadLocal<byte[]> BYTE_BUFFER = ThreadLocal.withInitial(IOUtil::newByteBuffer);

	/**
	 * Deny all constructions.
	 */
	private ThreadLocals() {
	}

	/**
	 * @return Thread-local byte buffer.
	 */
	public static byte[] getByteBuffer() {
		return BYTE_BUFFER.get();
	}
}
