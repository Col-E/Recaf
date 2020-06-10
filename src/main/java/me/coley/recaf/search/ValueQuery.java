package me.coley.recaf.search;

/**
 * Query to find constant values.
 *
 * @author Matt
 */
public class ValueQuery extends Query {
	private final Object value;

	/**
	 * Constructs a value matching query.
	 *
	 * @param value
	 * 		Value to search for.
	 */
	public ValueQuery(Object value) {
		super(QueryType.VALUE, null);
		this.value = value;
	}

	/**
	 * Adds a result if the given value matches the specified value.
	 *
	 * @param value
	 * 		Value to match.
	 */
	public void match(Object value) {
		if(this.value.equals(value)) {
			getMatched().add(new ValueResult(value));
		}
	}
}
