package software.coley.recaf.services.mapping.data;

import jakarta.annotation.Nonnull;

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
	public MethodMapping(@Nonnull String ownerName, @Nonnull String oldName,
						 @Nonnull String desc, @Nonnull String newName) {
		this.ownerName = ownerName;
		this.desc = desc;
		this.oldName = oldName;
		this.newName = newName;
	}

	@Nonnull
	@Override
	public String getOwnerName() {
		return ownerName;
	}

	@Nonnull
	@Override
	public String getDesc() {
		return desc;
	}

	@Nonnull
	@Override
	public String getOldName() {
		return oldName;
	}

	@Nonnull
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
		int result = ownerName.hashCode();
		result = 31 * result + desc.hashCode();
		result = 31 * result + oldName.hashCode();
		result = 31 * result + newName.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return oldName + " ==> " + newName;
	}
}
