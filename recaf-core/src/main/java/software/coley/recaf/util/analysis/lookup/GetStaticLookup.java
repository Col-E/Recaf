package software.coley.recaf.util.analysis.lookup;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.FieldInsnNode;
import software.coley.recaf.util.analysis.value.ReValue;

/**
 * Lookup for static field values.
 *
 * @author Matt Coley
 */
public interface GetStaticLookup {
	/**
	 * @param field
	 * 		Field reference.
	 *
	 * @return Value representing the field.
	 */
	@Nonnull
	ReValue get(@Nonnull FieldInsnNode field);
}
