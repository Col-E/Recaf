package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.UninitializedValue;

/**
 * Uninitialized value implementation.
 *
 * @author Matt Coley
 */
public class UninitializedValueImpl implements UninitializedValue {
	public static final UninitializedValue UNINITIALIZED_VALUE = new UninitializedValueImpl();

	private UninitializedValueImpl() {}

	@Override
	public String toString() {
		return ".";
	}

	@Nonnull
	@Override
	public ReValue mergeWith(@Nonnull ReValue other) {
		return UNINITIALIZED_VALUE;
	}
}
