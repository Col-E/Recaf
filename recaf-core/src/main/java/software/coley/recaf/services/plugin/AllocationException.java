package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;

/**
 * Wrapper exception for failures while allocating or releasing an instance through a {@link CdiClassAllocator}.
 *
 * @author Matt Coley
 */
public class AllocationException extends Exception {
	private final Class<?> type;

	/**
	 * @param type
	 * 		Type whose allocation or release cleanup failed.
	 * @param cause
	 * 		Reason for allocation or cleanup failure.
	 */
	public AllocationException(@Nonnull Class<?> type, @Nonnull Throwable cause) {
		super(cause);
		this.type = type;
	}

	/**
	 * @return Type whose allocation or cleanup failed.
	 */
	@Nonnull
	public Class<?> getType() {
		return type;
	}
}
