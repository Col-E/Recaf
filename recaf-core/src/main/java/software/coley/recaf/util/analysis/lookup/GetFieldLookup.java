package software.coley.recaf.util.analysis.lookup;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.FieldInsnNode;
import software.coley.recaf.util.analysis.value.ReValue;

/**
 * Lookup for context-bound field values.
 *
 * @author Matt Coley
 */
public interface GetFieldLookup {
	/**
	 * @param field
	 * 		Field reference.
	 * @param context
	 * 		Class context the field resides within.
	 * 		Has a {@link ReValue#hasKnownValue() known value}.
	 *
	 * @return Value representing the field.
	 */
	@Nonnull
	ReValue get(@Nonnull FieldInsnNode field, @Nonnull ReValue context);

	/**
	 * @param field
	 * 		Field reference.
	 *
	 * @return {@code true} when this lookup can provide a value via {@link #get(FieldInsnNode, ReValue)}.
	 */
	boolean hasLookup(@Nonnull FieldInsnNode field);
}
