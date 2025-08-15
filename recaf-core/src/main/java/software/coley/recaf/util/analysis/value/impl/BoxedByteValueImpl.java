package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.Nullness;

/**
 * Boxed {@link Byte} value holder implementation.
 *
 * @author Matt Coley
 */
public class BoxedByteValueImpl extends ObjectValueBoxImpl<Byte> {
	private static final Type TYPE = Type.getType(Byte.class);

	public BoxedByteValueImpl(@Nonnull Nullness nullness) {
		super(TYPE, nullness);
	}

	public BoxedByteValueImpl(@Nullable Byte value) {
		super(TYPE, value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Byte> wrap(@Nullable Byte value) {
		return new BoxedByteValueImpl(value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Byte> wrapUnknown(@Nonnull Nullness nullness) {
		return new BoxedByteValueImpl(nullness);
	}

	@Override
	public int getSize() {
		// Deconflict between object-v-primitive value interfaces
		return 1;
	}
}
