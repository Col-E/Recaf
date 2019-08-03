package me.coley.recaf.search;

import java.util.Objects;

/**
 * Search result of a matched class.
 *
 * @author Matt
 */
public class ClassResult extends SearchResult {
	private final int access;
	private final String name;

	/**
	 * Constructs a class result.
	 *
	 * @param access
	 * 		Class modifiers.
	 * @param name
	 * 		Name of class.
	 */
	public ClassResult(int access, String name) {
		this.access = access;
		this.name = name;
	}

	/**
	 * @return Class modifiers.
	 */
	public int getAccess() {
		return access;
	}

	/**
	 * @return Name of class.
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, access);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ClassResult)
			return hashCode() == other.hashCode();
		return false;
	}

	@Override
	public int compareTo(SearchResult other) {
		int ret = super.compareTo(other);
		if (ret == 0) {
			if (other instanceof ClassResult) {
				ClassResult otherResult = (ClassResult) other;
				return name.compareTo(otherResult.name);
			}
		}
		return ret;
	}
}
