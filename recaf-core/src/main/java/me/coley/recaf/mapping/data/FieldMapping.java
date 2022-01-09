package me.coley.recaf.mapping.data;

import java.util.Objects;

/**
 * Outlines mappings for a field.
 *
 * @author Matt Coley
 */
public class FieldMapping implements MemberMapping {
	private final String ownerName;
	private final String desc;
	private final String oldName;
	private final String newName;

	/**
	 * @param ownerName
	 * 		Name of class defining the field.
	 * @param oldName
	 * 		Pre-mapping field name.
	 * @param newName
	 * 		Post-mapping field name.
	 */
	public FieldMapping(String ownerName, String oldName, String newName) {
		this(ownerName, oldName, null, newName);
	}

	/**
	 * @param ownerName
	 * 		Name of class defining the field.
	 * @param oldName
	 * 		Pre-mapping field name.
	 * @param desc
	 * 		Descriptor type of the field.
	 * @param newName
	 * 		Post-mapping field name.
	 */
	public FieldMapping(String ownerName, String oldName, String desc, String newName) {
		this.ownerName = Objects.requireNonNull(ownerName, "Mapping entries cannot be null");
		this.oldName = Objects.requireNonNull(oldName, "Mapping entries cannot be null");
		this.newName = Objects.requireNonNull(newName, "Mapping entries cannot be null");
		// Except the descriptor, some mapping types do not use it.
		this.desc = desc;
	}

	@Override
	public String getOwnerName() {
		return ownerName;
	}

	@Override
	public String getDesc() {
		return desc;
	}

	@Override
	public String getOldName() {
		return oldName;
	}

	@Override
	public String getNewName() {
		return newName;
	}

	@Override
	public boolean isField() {
		return true;
	}

	/**
	 * @return {@code true} when there is no associated type from {@link #getDesc()}.
	 */
	public boolean hasType() {
		return desc != null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FieldMapping that = (FieldMapping) o;
		return ownerName.equals(that.ownerName) && Objects.equals(desc, that.desc)
				&& oldName.equals(that.oldName) && newName.equals(that.newName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ownerName, desc, oldName, newName);
	}

	@Override
	public String toString() {
		return oldName + " ==> " + newName;
	}
}
