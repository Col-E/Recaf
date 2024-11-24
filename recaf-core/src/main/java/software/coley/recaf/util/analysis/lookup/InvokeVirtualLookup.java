package software.coley.recaf.util.analysis.lookup;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.util.analysis.value.ReValue;

import java.util.List;

/**
 * Lookup for context-bound method return values.
 *
 * @author Matt Coley
 */
public interface InvokeVirtualLookup {
	/**
	 * @param method
	 * 		Method reference.
	 * @param context
	 * 		Class context the method resides within.
	 * @param values
	 * 		Argument values to the method.
	 *
	 * @return Value representing the return value of the method.
	 */
	@Nonnull
	ReValue get(@Nonnull MethodInsnNode method, @Nonnull ReValue context, @Nonnull List<? extends ReValue> values);
}
