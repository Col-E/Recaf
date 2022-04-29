package me.coley.recaf.search.result;

/**
 * Outline of location information of a parent {@link Result}.
 *
 * @author Matt Coley
 */
public interface Location extends Comparable<Location> {
	/**
	 * @return String format of the location path, allowing items to be sorted.
	 */
	String comparableString();

	@Override
	default int compareTo(Location other) {
		return comparableString().compareTo(other.comparableString());
	}
}
