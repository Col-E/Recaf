package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.StringValue;

import java.util.Optional;

/**
 * String value holder implementation.
 *
 * @author Matt Coley
 */
public class StringValueImpl extends ObjectValueBoxImpl<String> implements StringValue {
	public StringValueImpl(@Nonnull Nullness nullness) {
		super(Types.STRING_TYPE, nullness);
	}

	public StringValueImpl(@Nullable String value) {
		super(Types.STRING_TYPE, value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<String> wrap(@Nullable String value) {
		return new StringValueImpl(value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<String> wrapUnknown(@Nonnull Nullness nullness) {
		return new StringValueImpl(nullness);
	}

	@Nonnull
	@Override
	public Optional<String> getText() {
		return value();
	}
}
