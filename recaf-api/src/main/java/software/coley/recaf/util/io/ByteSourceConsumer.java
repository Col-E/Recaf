package software.coley.recaf.util.io;

import java.io.IOException;

/**
 * IO consumer for byte source.
 *
 * @param <E>
 * 		Additional argument type.
 *
 * @author xDark
 */
@FunctionalInterface
public interface ByteSourceConsumer<E> {
	/**
	 * Performs this operation on the given byte source.
	 *
	 * @param e
	 * 		Additional argument.
	 * @param source
	 * 		Byte source.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	void accept(E e, ByteSource source) throws IOException;
}
