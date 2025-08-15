package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.Nullness;

/**
 * Boxed {@link Long} value holder implementation.
 *
 * @author Matt Coley
 */
public class BoxedLongValueImpl extends ObjectValueBoxImpl<Long> {
	private static final Type TYPE = Type.getType(Long.class);

	public BoxedLongValueImpl(@Nonnull Nullness nullness) {
		super(TYPE, nullness);
	}

	public BoxedLongValueImpl(@Nullable Long value) {
		super(TYPE, value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Long> wrap(@Nullable Long value) {
		return new BoxedLongValueImpl(value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Long> wrapUnknown(@Nonnull Nullness nullness) {
		return new BoxedLongValueImpl(nullness);
	}

	@Override
	public int getSize() {
		// Deconflict between object-v-primitive value interfaces
		return 1;
	}
}
