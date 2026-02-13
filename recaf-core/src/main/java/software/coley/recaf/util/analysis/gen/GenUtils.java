package software.coley.recaf.util.analysis.gen;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.Types;

/**
 * Common utilities for code generation of mappers and method handlers.
 *
 * @author Matt Coley
 */
public class GenUtils {
	protected static boolean isSupportedType(@Nonnull Class<?> cls) {
		while (cls.isArray())
			cls = cls.getComponentType();
		if (cls == void.class) return false;
		return cls.isPrimitive() || cls == String.class || cls == CharSequence.class
				|| cls == Object.class
				|| Types.isBoxedPrimitive(Type.getDescriptor(cls));
	}

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

	@Nonnull
	protected static String toMapper(@Nonnull Class<?> cls) {
		return toMapper(cls, true);
	}

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
		if (cls != Object.class) return "BasicLookupUtils.<" + cls.getSimpleName() + ">obj";
		return objectLiteral ? "objl" : "obj";
	}
}
