package software.coley.recaf.services.mapping.data;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
	public FieldMapping(@Nonnull String ownerName, @Nonnull String oldName, @Nonnull String newName) {
		this(ownerName, oldName, null, newName);
	}

	/**
	 * @param ownerName
	 * 		Name of class defining the field.
	 * @param oldName
	 * 		Pre-mapping field name.
	 * @param desc
	 * 		Descriptor type of the field.
	 * 		May be {@code null} since not all formats use it.
	 * @param newName
	 * 		Post-mapping field name.
	 */
	public FieldMapping(@Nonnull String ownerName, @Nonnull String oldName, @Nullable String desc, @Nonnull String newName) {
		this.ownerName = ownerName;
		this.oldName = oldName;
		this.newName = newName;
		this.desc = desc;
	}

	@Nonnull
	@Override
	public String getOwnerName() {
		return ownerName;
	}

	@Nullable
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
