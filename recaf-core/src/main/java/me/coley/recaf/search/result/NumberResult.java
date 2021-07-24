package me.coley.recaf.search.result;

/**
 * Result containing some matched numeric value.
 *
 * @author Matt Coley
 */
public class NumberResult extends Result {
	private final Number matchedNumber;

	/**
	 * @param builder
	 * 		Builder containing information about the result.
	 * @param matchedNumber
	 * 		The numeric value matched.
	 */
	public NumberResult(ResultBuilder builder, Number matchedNumber) {
		super(builder);
		this.matchedNumber = matchedNumber;
	}

	/**
	 * @return The numeric value matched.
	 */
	public Number getMatchedNumber() {
		return matchedNumber;
	}

	@Override
	protected Object getValue() {
		return getMatchedNumber();
	}
}
