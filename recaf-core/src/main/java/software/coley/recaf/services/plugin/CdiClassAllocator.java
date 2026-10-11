package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;

/**
 * Responsible for creating new bean class instances from a {@link Class} reference.
 *
 * @author Matt Coley
 */
public interface CdiClassAllocator {
	/**
	 * Creates a new instance of a class, allocating any CDI resources needed for the instance to function.
	 * <p>
	 * <b>NOTE</b>: The caller must eventually pass it to {@link #release(Object)}.
	 *
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

	/**
	 * Releases the CDI resources associated with an instance returned by {@link #instance(Class)}.
	 *
	 * @param instance
	 * 		Instance to release.
	 *
	 * @throws AllocationException
	 * 		When lifecycle cleanup fails.
	 */
	void release(@Nonnull Object instance) throws AllocationException;
}
