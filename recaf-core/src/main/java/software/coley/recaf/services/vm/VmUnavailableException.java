package software.coley.recaf.services.vm;

import jakarta.annotation.Nonnull;

/**
 * Thrown when the VM is not available for operations in {@link VirtualInvoker} and {@link VirtualOptimizer}.
 *
 * @author Matt Coley
 */
public class VmUnavailableException extends Exception {
	/**
	 * @param message
	 * 		Additional details.
	 * @param cause
	 * 		Cause for the VM to not be available. Likely due to an error in initialization.
	 */
	public VmUnavailableException(@Nonnull String message, @Nonnull Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * 		Additional details.
	 */
	public VmUnavailableException(@Nonnull String message) {
		super(message);
	}

	/**
	 * @param cause
	 * 		Cause for the VM to not be available. Likely due to an error in initialization.
	 */
	public VmUnavailableException(@Nonnull Throwable cause) {
		super(cause);
	}
}