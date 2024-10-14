package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;

/**
 * Wrapper exception for any potential error thrown by the implementation logic of a {@link ClassAllocator}
 *
 * @author Matt Coley
 */
public class AllocationException extends Exception {
	private final Class<?> type;

	/**
	 * @param type
	 * 		Type that failed to be allocated.
	 * @param cause
	 * 		Reason for allocation failure.
	 */
	public AllocationException(@Nonnull Class<?> type, @Nonnull Throwable cause) {
		super(cause);
		this.type = type;
	}

	/**
	 * @return Type that failed to be allocated.
	 */
	@Nonnull
	public Class<?> getType() {
		return type;
	}
}
