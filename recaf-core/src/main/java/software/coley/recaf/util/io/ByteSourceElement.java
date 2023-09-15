package software.coley.recaf.util.io;

/**
 * Container for an element and a byte source.
 *
 * @author xDark
 */
public class ByteSourceElement<E> {
	private final E element;
	private final ByteSource byteSource;

	/**
	 * @param element
	 * 		Element value.
	 * @param byteSource
	 * 		Byte source.
	 */
	public ByteSourceElement(E element, ByteSource byteSource) {
		this.element = element;
		this.byteSource = byteSource;
	}

	/**
	 * @return Element value.
	 */
	public E getElement() {
		return element;
	}

	/**
	 * @return Byte source.
	 */
	public ByteSource getByteSource() {
		return byteSource;
	}
}
