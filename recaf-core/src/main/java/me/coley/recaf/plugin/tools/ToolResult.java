package me.coley.recaf.plugin.tools;

/**
 * Wrapper for tool operation results.
 *
 * @param <T>
 * 		Tool implementation type.
 * @param <V>
 * 		Result value type.
 *
 * @author Matt Coley
 */
public class ToolResult<T, V> {
	private final T tool;
	private final V value;
	private final Throwable exception;

	/**
	 * Depending on if the operation was a success or not, one parameter or another will likely be {@code null}.
	 *
	 * @param value
	 * 		Tool operation result.
	 * @param exception
	 * 		Error thrown when attempting to run the tool.
	 */
	protected ToolResult(T tool, V value, Throwable exception) {
		this.tool = tool;
		this.value = value;
		this.exception = exception;
	}

	/**
	 * @return Tool that created the result.
	 */
	public T getTool() {
		return tool;
	}

	/**
	 * @return Tool operation result.
	 */
	public V getValue() {
		return value;
	}


	/**
	 * Indicates if the result contains either a {@link #getValue() result}
	 * or {@link #getException() error}.
	 *
	 * @return {@code true} if {@link #getValue()} exists.
	 */
	public boolean wasSuccess() {
		return getValue() != null;
	}

	/**
	 * @return Error thrown when attempting to run the tool. May be {@code null} if operation was a success.
	 */
	public Throwable getException() {
		return exception;
	}
}
