package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.Nullness;

/**
 * Boxed {@link Float} value holder implementation.
 *
 * @author Matt Coley
 */
public class BoxedFloatValueImpl extends ObjectValueBoxImpl<Float> {
	private static final Type TYPE = Type.getType(Float.class);

	public BoxedFloatValueImpl(@Nonnull Nullness nullness) {
		super(TYPE, nullness);
	}

	public BoxedFloatValueImpl(@Nullable Float value) {
		super(TYPE, value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Float> wrap(@Nullable Float value) {
		return new BoxedFloatValueImpl(value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Float> wrapUnknown(@Nonnull Nullness nullness) {
		return new BoxedFloatValueImpl(nullness);
	}

	@Override
	public int getSize() {
		// Deconflict between object-v-primitive value interfaces
		return 1;
	}
}
