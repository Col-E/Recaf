package software.coley.recaf.services.mapping.data;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface MemberMapping {
	/**
	 * @return Name of class defining the member.
	 */
	@Nonnull
	String getOwnerName();

	/**
	 * @return Descriptor type of the member.
	 * May be {@code null} for fields with some mapping implementations.
	 */
	@Nullable
	String getDesc();

	/**
	 * @return Pre-mapping member name.
	 */
	@Nonnull
	String getOldName();

	/**
	 * @return Post-mapping member name.
	 */
	@Nonnull
	String getNewName();

	/**
	 * @return {@code true} when the member is a field.
	 * {@code false} for methods.
	 */
	boolean isField();
}
