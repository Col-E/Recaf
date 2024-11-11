package software.coley.recaf.util.analysis.value;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.Value;

/**
 * Base type of value capable of recording exact content
 * when all control flow paths converge on a single use case.
 *
 * @author Matt Coley
 */
public sealed interface ReValue extends Value permits IntValue, FloatValue, DoubleValue, LongValue, ObjectValue, UninitializedValue {
	/**
	 * @return {@code true} when the exact content is known.
	 */
	boolean hasKnownValue();

	/**
	 * @return Type of value content.
	 */
	@Nullable
	Type type();

	/**
	 * @param other
	 * 		Other value to merge with.
	 *
	 * @return Merged value.
	 */
	@Nonnull
	ReValue mergeWith(@Nonnull ReValue other);
}
