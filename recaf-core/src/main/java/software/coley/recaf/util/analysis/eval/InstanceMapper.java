package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.analysis.gen.InstanceMapperGenerator;
import software.coley.recaf.util.analysis.value.ReValue;

import java.util.List;

/**
 * Outline for creating objects from {@link ReValue} items taken off the stack.
 * Represents a call to a constructor or static factory method that creates an instance of an object.
 *
 * @author Matt Coley
 * @see InstancedObjectValue#setRealInstance(Object)
 * @see InstanceMapperGenerator
 */
public interface InstanceMapper {
	/**
	 * @param host
	 * 		Object value to be populated with a real instance.
	 * 		For constructors, this will be the object being constructed.
	 * 		For static factories, this will be a dummy object of the correct type.
	 * @param parameters
	 * 		Parameters passed to the constructor or static factory method.
	 * 		These are the values on the stack at the time of the call.
	 *
	 * @return Real constructed instance to back the given object value.
	 *
	 * @throws Throwable
	 * 		When any error occurs during the mapping process.
	 */
	@Nonnull
	Object map(@Nonnull InstancedObjectValue<?> host, @Nonnull List<ReValue> parameters) throws Throwable;
}
