package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.Nullness;

/**
 * Boxed {@link Integer} value holder implementation.
 *
 * @author Matt Coley
 */
public class BoxedIntegerValueImpl extends ObjectValueBoxImpl<Integer> {
	private static final Type TYPE = Type.getType(Integer.class);

	public BoxedIntegerValueImpl(@Nonnull Nullness nullness) {
		super(TYPE, nullness);
	}

	public BoxedIntegerValueImpl(@Nullable Integer value) {
		super(TYPE, value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Integer> wrap(@Nullable Integer value) {
		return new BoxedIntegerValueImpl(value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Integer> wrapUnknown(@Nonnull Nullness nullness) {
		return new BoxedIntegerValueImpl(nullness);
	}

	@Override
	public int getSize() {
		// Deconflict between object-v-primitive value interfaces
		return 1;
	}
}
