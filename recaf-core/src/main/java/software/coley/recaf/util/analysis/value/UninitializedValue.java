package software.coley.recaf.util.analysis.value;

import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.value.impl.UninitializedValueImpl;

/**
 * Value representing a slot that has not been initialized or used yet.
 *
 * @author Matt Coley
 */
public non-sealed interface UninitializedValue extends ReValue {
	UninitializedValue UNINITIALIZED_VALUE = UninitializedValueImpl.UNINITIALIZED_VALUE;

	@Override
	default boolean hasKnownValue() {
		return false;
	}

	@Nullable
	@Override
	default Type type() {
		return null;
	}

	@Override
	default int getSize() {
		return 1;
	}
}
