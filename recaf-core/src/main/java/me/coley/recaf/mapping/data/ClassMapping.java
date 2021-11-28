package me.coley.recaf.mapping.data;

import java.util.Objects;

/**
 * Outlines mappings for a class.
 *
 * @author Matt Coley
 */
public class ClassMapping {
	private final String oldName;
	private final String newName;

	/**
	 * @param oldName
	 * 		Pre-mapping name.
	 * @param newName
	 * 		Post-mapping name.
	 */
	public ClassMapping(String oldName, String newName) {
		this.oldName = Objects.requireNonNull(oldName, "Mapping entries cannot be null");
		this.newName = Objects.requireNonNull(newName, "Mapping entries cannot be null");
	}

	/**
	 * @return Pre-mapping name.
	 */
	public String getOldName() {
		return oldName;
	}

	/**
	 * @return Post-mapping name.
	 */
	public String getNewName() {
		return newName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ClassMapping that = (ClassMapping) o;
		return oldName.equals(that.oldName) && newName.equals(that.newName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(oldName, newName);
	}

	@Override
	public String toString() {
		return oldName + " ==> " + newName;
	}
}
