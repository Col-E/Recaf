package software.coley.recaf.util.io;

import software.coley.recaf.util.ReflectUtil;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.file.Path;
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
	public static <E> Consumer<ByteSourceElement<E>> consume(ByteSourceConsumer<E> consumer) {
		return e -> {
			try {
				consumer.accept(e.getElement(), e.getByteSource());
			} catch (IOException ex) {
				ReflectUtil.propagate(ex);
			}
		};
	}

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
	public static ByteSource wrap(byte[] bytes, int offset, int length) {
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
	public static ByteSource wrap(byte[] bytes) {
		return new ByteArraySource(bytes, 0, bytes.length);
	}

	/**
	 * Creates new byte source from byte buffer.
	 *
	 * @param buffer
	 * 		Buffer to wrap.
	 *
	 * @return New byte source.
	 */
	public static ByteSource forBuffer(ByteBuffer buffer) {
		return new ByteBufferSource(buffer);
	}

	/**
	 * Creates new byte source from path.
	 *
	 * @param path
	 * 		Path to read bytes from.
	 *
	 * @return New byte source.
	 */
	public static ByteSource forPath(Path path) {
		return new PathByteSource(path);
	}

	/**
	 * Creates new byte source from lljzip byte data.
	 *
	 * @param data
	 * 		Data to create source for.
	 *
	 * @return New byte source.
	 */
	public static ByteSource forMemorySegment(MemorySegment data) {
		return new MemorySegmentDataSource(data);
	}
}
