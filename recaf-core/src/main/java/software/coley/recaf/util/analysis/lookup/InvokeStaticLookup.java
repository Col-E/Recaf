package software.coley.recaf.util.analysis.lookup;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.util.analysis.value.ReValue;

import java.util.List;

/**
 * Lookup for static method return values.
 *
 * @author Matt Coley
 */
public interface InvokeStaticLookup {
	/**
	 * @param method
	 * 		Method reference.
	 * @param values
	 * 		Argument values to the method.
	 *
	 * @return Value representing the return value of the method.
	 */
	@Nonnull
	ReValue get(@Nonnull MethodInsnNode method, @Nonnull List<? extends ReValue> values);
}
