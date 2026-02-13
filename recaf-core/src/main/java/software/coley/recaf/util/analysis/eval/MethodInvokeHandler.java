package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.analysis.gen.InstanceMethodInvokeHandlerGenerator;
import software.coley.recaf.util.analysis.value.ReValue;

import java.util.List;

/**
 * Handler for invoking methods on real instances of objects.
 *
 * @param <T>
 * 		Type of the real instance.
 *
 * @author Matt Coley
 * @see InstanceMethodInvokeHandlerGenerator
 */
public interface MethodInvokeHandler<T> {
	/**
	 * @param receiverValue
	 * 		Value of the receiver on the stack.
	 * 		This is used to {@link FieldCacheManager#getInstanceFieldCache(ReValue) track field values}
	 * 		and other state on the instance.
	 * @param receiver
	 * 		Real instance of the object the method is being called on.
	 * @param args
	 * 		Values of the arguments on the stack passed to the method.
	 *
	 * @return Value returned by the method, or {@code null} if the method is {@code void}.
	 *
	 * @throws Throwable
	 * 		When any error occurs during method invocation.
	 */
	@Nullable
	ReValue invoke(@Nonnull ReValue receiverValue, @Nonnull T receiver, @Nonnull List<ReValue> args) throws Throwable;
}