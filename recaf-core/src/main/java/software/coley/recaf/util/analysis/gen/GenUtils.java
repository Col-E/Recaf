package software.coley.recaf.util.analysis.gen;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.Types;

import java.nio.Buffer;
import java.nio.ByteOrder;

/**
 * Common utilities for code generation of mappers and method handlers.
 *
 * @author Matt Coley
 */
public class GenUtils {
	/**
	 * Checks whether a type is supported for mapping.
	 *
	 * @param cls
	 * 		Type to inspect.
	 *
	 * @return {@code true} when the type is supported for mapping.
	 */
	protected static boolean isSupportedType(@Nonnull Class<?> cls) {
		if (isHostObjectType(cls))
			return true;
		while (cls.isArray())
			cls = cls.getComponentType();
		if (cls == void.class) return false;
		return cls.isPrimitive() || cls == String.class || cls == CharSequence.class
				|| cls == Object.class
				|| Types.isBoxedPrimitive(Type.getDescriptor(cls));
	}

	/**
	 * Checks whether a type is represented by a host-backed evaluator object.
	 *
	 * @param cls
	 * 		Type to inspect.
	 *
	 * @return {@code true} when the type must be unwrapped from a host-backed value.
	 */
	protected static boolean isHostObjectType(@Nonnull Class<?> cls) {
		return cls == ByteOrder.class || Buffer.class.isAssignableFrom(cls);
	}

	/**
	 * Converts a host type to the evaluator value type.
	 *
	 * @param cls
	 * 		Type to convert.
	 *
	 * @return Evaluator value type.
	 */
	@Nonnull
	protected static String toValue(@Nonnull Class<?> cls) {
		if (cls.isArray()) {
			return "ArrayValue";
		} else if (cls.isPrimitive()) {
			if (cls == short.class || cls == char.class || cls == byte.class || cls == boolean.class)
				return "IntValue";
			return StringUtil.uppercaseFirstChar(cls.getSimpleName()) + "Value";
		} else if (cls == String.class || cls == CharSequence.class) {
			return "StringValue";
		} else {
			return "ObjectValue";
		}
	}

	/**
	 * Converts a host type to the evaluator value type mapper.
	 *
	 * @param cls
	 * 		Type to convert.
	 *
	 * @return Evaluator value type mapper.
	 */
	@Nonnull
	protected static String toMapper(@Nonnull Class<?> cls) {
		return toMapper(cls, true);
	}

	/**
	 * Converts a host type to the evaluator value type mapper.
	 *
	 * @param cls
	 * 		Type to convert.
	 * @param objectLiteral
	 * 		Whether the mapper is being used for an object literal.
	 *
	 * @return Evaluator value type mapper.
	 */
	protected static String toMapper(@Nonnull Class<?> cls, boolean objectLiteral) {
		if (cls == String.class || cls == CharSequence.class) return "str";
		if (cls == boolean.class) return "z";
		if (cls == byte.class) return "b";
		if (cls == char.class) return "c";
		if (cls == short.class) return "s";
		if (cls == int.class) return "i";
		if (cls == long.class) return "j";
		if (cls == float.class) return "f";
		if (cls == double.class) return "d";
		if (cls.isArray()) return "arr" + toMapper(cls.componentType(), false);
		if (isHostObjectType(cls)) return "requireRealInstance";
		if (cls != Object.class) return "BasicLookupUtils.<" + cls.getSimpleName() + ">obj";
		return objectLiteral ? "objl" : "obj";
	}

	/**
	 * Builds the evaluator-to-host conversion for a method argument.
	 *
	 * @param cls
	 * 		Host parameter type.
	 * @param expression
	 * 		Evaluator value expression.
	 *
	 * @return Generated argument conversion expression.
	 */
	@Nonnull
	protected static String toParameterMapper(@Nonnull Class<?> cls, @Nonnull String expression) {
		if (isHostObjectType(cls))
			return "requireRealInstance(" + expression + ", " + cls.getSimpleName() + ".class)";
		return toMapper(cls) + "((" + toValue(cls) + ")" + expression + ")";
	}
}
