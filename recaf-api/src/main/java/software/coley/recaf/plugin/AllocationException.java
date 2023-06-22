package software.coley.recaf.plugin;

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
	public AllocationException(Class<?> type, Throwable cause) {
		super(cause);
		this.type = type;
	}

	/**
	 * @return Type that failed to be allocated.
	 */
	public Class<?> getType() {
		return type;
	}
}
