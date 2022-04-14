package me.coley.recaf.io;

import me.coley.recaf.util.ReflectUtil;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Byte source utilities.
 *
 * @author xDark
 */
public class ByteSources {
	/**
	 * Deny all constructions.
	 */
	private ByteSources() {
	}

	/**
	 * Wraps {@link ByteSourceConsumer} into a consumer
	 * that accepts {@link ByteSourceElement}.
	 *
	 * @param consumer
	 * 		Consumer to wrap.
	 * @param <E>
	 * 		Element type.
	 *
	 * @return Wrapped consumer.
	 */
	public static <E> Consumer<ByteSourceElement<E>> from(ByteSourceConsumer<E> consumer) {
		return e -> {
			try {
				consumer.accept(e.getElement(), e.getByteSource());
			} catch (IOException ex) {
				ReflectUtil.propagate(ex);
			}
		};
	}
}
