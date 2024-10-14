package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;

/**
 * Responsible for creating new class instances from a {@link Class} reference.
 *
 * @author Matt Coley
 */
public interface ClassAllocator {
	/**
	 * @param cls
	 * 		Class to allocate an instance of.
	 * @param <T>
	 * 		Type of class.
	 *
	 * @return Instance of T type.
	 *
	 * @throws AllocationException
	 * 		When any error prevents a new instance from being provided.
	 */
	@Nonnull
	<T> T instance(@Nonnull Class<T> cls) throws AllocationException;
}
