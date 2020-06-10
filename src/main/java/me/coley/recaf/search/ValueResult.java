package me.coley.recaf.search;

/**
 * Search result of some value.
 *
 * @author Matt
 */
public class ValueResult extends SearchResult {
	private final Object value;

	/**
	 * Constructs a value result.
	 *
	 * @param value
	 * 		Matched value.
	 */
	public ValueResult(Object value) {
		this.value = value;
	}

	/**
	 * @return Matched value.
	 */
	public Object getValue() {
		return value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public int compareTo(SearchResult other) {
		int ret = super.compareTo(other);
		if (ret == 0) {
			if (other instanceof ValueResult) {
				ValueResult otherResult = (ValueResult) other;
				if (value instanceof Comparable && otherResult.value instanceof Comparable) {
					Comparable compValue = (Comparable) value;
					Comparable compOtherValue = (Comparable) otherResult.value;
					return compValue.compareTo(compOtherValue);
				}
			}
		}
		return ret;
	}
}
