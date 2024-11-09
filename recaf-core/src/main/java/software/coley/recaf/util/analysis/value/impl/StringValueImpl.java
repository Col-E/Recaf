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
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class StringValueImpl extends ObjectValueImpl implements StringValue {
	private final Optional<String> text;

	public StringValueImpl(@Nonnull Nullness nullness) {
		super(Types.STRING_TYPE, nullness);
		text = Optional.empty();
	}

	public StringValueImpl(@Nullable String text) {
		super(Types.STRING_TYPE, text != null ? Nullness.NOT_NULL : Nullness.UNKNOWN);
		this.text = Optional.ofNullable(text);
	}

	@Nonnull
	@Override
	public Optional<String> getText() {
		return text;
	}

	@Override
	public boolean hasKnownValue() {
		return nullness() == Nullness.NOT_NULL && text.isPresent();
	}
}
