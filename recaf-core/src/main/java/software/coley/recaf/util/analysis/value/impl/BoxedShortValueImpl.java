package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.Nullness;

/**
 * Boxed {@link Short} value holder implementation.
 *
 * @author Matt Coley
 */
public class BoxedShortValueImpl extends ObjectValueBoxImpl<Short> {
	private static final Type TYPE = Type.getType(Short.class);

	public BoxedShortValueImpl(@Nonnull Nullness nullness) {
		super(TYPE, nullness);
	}

	public BoxedShortValueImpl(@Nullable Short value) {
		super(TYPE, value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Short> wrap(@Nullable Short value) {
		return new BoxedShortValueImpl(value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Short> wrapUnknown(@Nonnull Nullness nullness) {
		return new BoxedShortValueImpl(nullness);
	}

	@Override
	public int getSize() {
		// Deconflict between object-v-primitive value interfaces
		return 1;
	}
}
