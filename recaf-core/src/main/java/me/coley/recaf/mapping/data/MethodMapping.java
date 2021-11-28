package me.coley.recaf.mapping.data;

import java.util.Objects;

/**
 * Outlines mappings for a method.
 *
 * @author Matt Coley
 */
public class MethodMapping implements MemberMapping {
	private final String ownerName;
	private final String desc;
	private final String oldName;
	private final String newName;

	/**
	 * @param ownerName
	 * 		Name of class defining the method.
	 * @param oldName
	 * 		Pre-mapping method name.
	 * @param desc
	 * 		Descriptor type of the method.
	 * @param newName
	 * 		Post-mapping method name.
	 */
	public MethodMapping(String ownerName, String oldName, String desc, String newName) {
		this.ownerName = Objects.requireNonNull(ownerName, "Mapping entries cannot be null");
		this.desc = Objects.requireNonNull(desc, "Mapping entries cannot be null");
		this.oldName = Objects.requireNonNull(oldName, "Mapping entries cannot be null");
		this.newName = Objects.requireNonNull(newName, "Mapping entries cannot be null");
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
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MethodMapping that = (MethodMapping) o;
		return ownerName.equals(that.ownerName) && desc.equals(that.desc)
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
