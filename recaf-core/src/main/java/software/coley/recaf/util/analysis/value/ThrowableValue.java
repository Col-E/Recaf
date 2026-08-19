package software.coley.recaf.util.analysis.value;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Value modeling thrown exceptions.
 *
 * @author Matt Coley
 */
public interface ThrowableValue extends ObjectValue {
	/**
	 * @return Stack trace of the evaluated code, captured at throwable construction time.
	 */
	@Nonnull
	List<StackTraceElement> getStackTrace();

	/**
	 * @return Host exception which caused this value, or {@code null} for evaluator-created faults.
	 */
	@Nullable
	Throwable getBackingException();
}
