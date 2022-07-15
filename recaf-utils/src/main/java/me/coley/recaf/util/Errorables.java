package me.coley.recaf.util;

/**
 * Convenience calls for the error-able lambda types.
 */
public class Errorables {
	/**
	 * @param errorable
	 * 		Runnable.
	 */
	public static void silent(Errorable errorable) {
		try {
			errorable.run();
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/**
	 * @param errorable
	 * 		Supplier.
	 * @param <T>
	 * 		Supplier type.
	 *
	 * @return Supplied value.
	 */
	public static <T> T silent(ErrorableSupplier<T> errorable) {
		try {
			return errorable.get();
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/**
	 * @param errorable
	 * 		Consumer.
	 * @param value
	 * 		Consumed value.
	 * @param <T>
	 * 		Consumer type.
	 */
	public static <T> void silent(ErrorableConsumer<T> errorable, T value) {
		try {
			errorable.accept(value);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}
}
