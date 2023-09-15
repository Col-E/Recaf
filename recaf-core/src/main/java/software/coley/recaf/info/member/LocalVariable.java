package software.coley.recaf.info.member;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Local variable component of a {@link MethodMember}.
 *
 * @author Matt Coley
 */
public interface LocalVariable {
	/**
	 * @return Variable index.
	 */
	int getIndex();

	/**
	 * @return Variable name.
	 */
	@Nonnull
	String getName();

	/**
	 * @return Variable type.
	 */
	@Nonnull
	String getDescriptor();

	/**
	 * @return Variable generic type.
	 */
	@Nullable
	String getSignature();
}
