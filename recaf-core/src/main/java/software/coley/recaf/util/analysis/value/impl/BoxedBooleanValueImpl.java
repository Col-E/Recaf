package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.Nullness;

/**
 * Boxed {@link Boolean} value holder implementation.
 *
 * @author Matt Coley
 */
public class BoxedBooleanValueImpl extends ObjectValueBoxImpl<Boolean> {
	private static final Type TYPE = Type.getType(Boolean.class);

	public BoxedBooleanValueImpl(@Nonnull Nullness nullness) {
		super(TYPE, nullness);
	}

	public BoxedBooleanValueImpl(@Nullable Boolean value) {
		super(TYPE, value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Boolean> wrap(@Nullable Boolean value) {
		return new BoxedBooleanValueImpl(value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Boolean> wrapUnknown(@Nonnull Nullness nullness) {
		return new BoxedBooleanValueImpl(nullness);
	}

	@Override
	public int getSize() {
		// Deconflict between object-v-primitive value interfaces
		return 1;
	}
}
