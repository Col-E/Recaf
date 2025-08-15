package software.coley.recaf.util.analysis.value.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.Nullness;

/**
 * Boxed {@link Character} value holder implementation.
 *
 * @author Matt Coley
 */
public class BoxedCharacterValueImpl extends ObjectValueBoxImpl<Character> {
	private static final Type TYPE = Type.getType(Character.class);

	public BoxedCharacterValueImpl(@Nonnull Nullness nullness) {
		super(TYPE, nullness);
	}

	public BoxedCharacterValueImpl(@Nullable Character value) {
		super(TYPE, value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Character> wrap(@Nullable Character value) {
		return new BoxedCharacterValueImpl(value);
	}

	@Nonnull
	@Override
	protected ObjectValueBoxImpl<Character> wrapUnknown(@Nonnull Nullness nullness) {
		return new BoxedCharacterValueImpl(nullness);
	}

	@Override
	public int getSize() {
		// Deconflict between object-v-primitive value interfaces
		return 1;
	}
}
