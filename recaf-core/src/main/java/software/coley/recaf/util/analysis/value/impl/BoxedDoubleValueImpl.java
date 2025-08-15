package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.Nullness;

/**
 * Boxed {@link Double} value holder implementation.
 *
 * @author Matt Coley
 */
public class BoxedDoubleValueImpl extends ObjectValueBoxImpl<Double> {
	private static final Type TYPE = Type.getType(Double.class);

	public BoxedDoubleValueImpl(@Nonnull Nullness nullness) {
		super(TYPE, nullness);
	}

	public BoxedDoubleValueImpl(@Nullable Double value) {
		super(TYPE, value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Double> wrap(@Nullable Double value) {
		return new BoxedDoubleValueImpl(value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Double> wrapUnknown(@Nonnull Nullness nullness) {
		return new BoxedDoubleValueImpl(nullness);
	}

	@Override
	public int getSize() {
		// Deconflict between object-v-primitive value interfaces
		return 1;
	}
}
