package software.coley.recaf.util.collect.primitive;

import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.List;

/**
 * A {@link List}-like collection for primitive {@code int} values.
 *
 * @author Matt Coley
 */
public class IntList {
	private static final int DEFAULT_CAPACITY = 16;

	private int[] data;
	private int size;

	/**
	 * New list with default capacity of 16.
	 */
	public IntList() {
		this(DEFAULT_CAPACITY);
	}

	/**
	 * New list with specified initial capacity.
	 *
	 * @param initialCapacity
	 * 		Initial capacity of the list.
	 */
	public IntList(int initialCapacity) {
		if (initialCapacity < 0)
			initialCapacity = DEFAULT_CAPACITY;
		this.data = new int[initialCapacity];
		this.size = 0;
	}

	/**
	 * @param value
	 * 		Value to add.
	 */
	public void add(int value) {
		ensureCapacity(size + 1);
		data[size++] = value;
	}

	/**
	 * @param index
	 * 		Index of the value to retrieve.
	 *
	 * @return Value at the specified index.
	 */
	public int get(int index) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException();
		return data[index];
	}

	/**
	 * @param index
	 * 		Index of the value to set.
	 * @param value
	 * 		Value to set at the specified index.
	 */
	public void set(int index, int value) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException();
		data[index] = value;
	}

	/**
	 * @param index
	 * 		Index of the value to remove.
	 *
	 * @return Value that was removed.
	 */
	public int removeAt(int index) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException();
		int removed = data[index];
		System.arraycopy(data, index + 1, data, index, size - index - 1);
		size--;
		return removed;
	}

	/**
	 * @return Number of elements in the list.
	 */
	public int size() {
		return size;
	}

	/**
	 * @return {@code true} when the list has zero elements.
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Removes all elements from the list.
	 */
	public void clear() {
		size = 0;
	}

	/**
	 * @return Array containing all elements in the list.
	 */
	@Nonnull
	public int[] toArray() {
		return Arrays.copyOf(data, size);
	}

	private void ensureCapacity(int minCapacity) {
		if (minCapacity > data.length) {
			int newCapacity = Math.max(minCapacity, data.length * 2);
			data = Arrays.copyOf(data, newCapacity);
		}
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof IntList other))
			return false;
		if (size != other.size)
			return false;
		for (int i = 0; i < size; i++)
			if (data[i] != other.data[i])
				return false;
		return true;
	}

	@Override
	public int hashCode() {
		int result = Integer.hashCode(size);
		for (int i = 0; i < size; i++)
			result = 31 * result + Integer.hashCode(data[i]);
		return result;
	}
}