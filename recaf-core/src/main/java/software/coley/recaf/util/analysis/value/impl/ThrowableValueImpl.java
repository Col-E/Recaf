package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.IllegalValueException;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.ThrowableValue;

import java.util.List;

/**
 * Throwable value holder implementation.
 *
 * @author Matt Coley
 */
public class ThrowableValueImpl extends ObjectValueImpl implements ThrowableValue {
	private final List<StackTraceElement> stackTrace;
	@Nullable
	private final Throwable backingException;

	public ThrowableValueImpl(@Nonnull Type type,
	                          @Nonnull List<StackTraceElement> stackTrace,
	                          @Nullable Throwable backingException) {
		super(type, Nullness.NOT_NULL);
		this.stackTrace = List.copyOf(stackTrace);
		this.backingException = backingException;
	}

	@Nonnull
	@Override
	public List<StackTraceElement> getStackTrace() {
		return stackTrace;
	}

	@Nullable
	@Override
	public Throwable getBackingException() {
		return backingException;
	}

	@Override
	public boolean hasKnownValue() {
		return true;
	}

	@Nonnull
	@Override
	public ReValue mergeWith(@Nonnull ReValue other) throws IllegalValueException {
		if (other == this || other instanceof ThrowableValue value && stackTrace.equals(value.getStackTrace()))
			return this;
		return super.mergeWith(other);
	}
}
