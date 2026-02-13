package software.coley.recaf.util.analysis.gen;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.lookup.BasicInvokeStaticLookup;
import software.coley.recaf.util.analysis.lookup.BasicInvokeVirtualLookup;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 * Generator for items in {@link BasicInvokeStaticLookup} and {@link BasicInvokeVirtualLookup}.
 *
 * @author Matt Coley
 */
public class LookupGenerator extends GenUtils {
	static boolean emitStatic = false;
	static Class<?>[] emitTargets = new Class[]{
			System.class,
			Arrays.class,
			String.class,
			CharSequence.class,
			Math.class,
			Boolean.class,
			Byte.class,
			Character.class,
			Short.class,
			Integer.class,
			Long.class,
			Float.class,
			Double.class,
			Number.class,
			Objects.class
	};

	/**
	 * Generator for method definitions.
	 */
	public static void main(String[] args) {
		for (Class<?> type : emitTargets) {
			System.out.println("// " + type.getName());
			for (Method method : type.getDeclaredMethods()) {
				// Skip inaccessible methods
				int modifiers = method.getModifiers();
				if (!Modifier.isPublic(modifiers))
					continue;
				if (emitStatic && !Modifier.isStatic(modifiers))
					continue;
				if (!emitStatic && Modifier.isStatic(modifiers))
					continue;

				// Skip static initializer and constructors
				String methodName = method.getName();
				if (method.getName().charAt(0) == '<')
					continue;

				// Skip if we don't support the return type
				if (!isSupportedType(method.getReturnType()))
					continue;

				// Skip if we don't support a parameter type
				boolean supportedParameters = true;
				Class<?>[] parameterTypes = method.getParameterTypes();
				for (Class<?> parameterType : parameterTypes) {
					if (!isSupportedType(parameterType)) {
						supportedParameters = false;
						break;
					}
				}
				if (!supportedParameters)
					continue;

				// Skip if we don't support the instance type on non-static methods
				if (!emitStatic && !isSupportedType(type))
					continue;

				// Build final output
				String template = buildTemplate(type, method);
				System.out.println(template);
			}
			System.out.println("\n\n");
		}
	}

	@Nonnull
	private static String buildTemplate(@Nonnull Class<?> type, @Nonnull Method method) {
		String typeName = Type.getInternalName(type);
		String methodName = method.getName();
		String wrapFunc = toMapper(method.getReturnType());
		String methodContext = emitStatic ? type.getSimpleName() : toMapper(type) + "(ctx)";
		Class<?>[] parameterTypes = method.getParameterTypes();
		int parameterCount = parameterTypes.length;
		Function<Integer, String> valueizer = i -> {
			if (!emitStatic) {
				i--;

				// 0th parameter becomes instance type of the method owner
				if (i < 0)
					return toValue(type);
			}
			return toValue(parameterTypes[i]);
		};
		Function<Integer, String> namer = i -> {
			if (!emitStatic) {
				i--;

				// 0th parameter becomes reference to the method owner
				if (i < 0)
					return "ctx";
			}
			return String.valueOf((char) ('a' + i));
		};
		Function<Integer, String> parameterizer = i -> {
			// Does not need to be adjusted for virtual vs static, already done in usage
			Class<?> parameterType = parameterTypes[i];
			String mapped = toMapper(parameterType);

			return mapped;
		};

		// METHODS.put("java/lang/Math.max(II)I",
		String methodDescriptor = Type.getMethodDescriptor(method);
		StringBuilder sb = new StringBuilder("METHODS.put(\"");
		sb.append(typeName).append(".").append(methodName).append(methodDescriptor).append("\", ");


		// (Func_1<IntValue>) (a)
		// (Func_2<IntValue, IntValue>) (a, b)
		// (Func_3<IntValue, IntValue, IntValue>) (a, b, c)
		switch (parameterCount + (emitStatic ? 0 : 1)) {
			case 0 -> {
				sb.append("(Func_0) () -> ");
			}
			case 1 -> {
				String typeA = valueizer.apply(0);
				sb.append("(Func_1<")
						.append(typeA).append(">) (")
						.append(namer.apply(0)).append(") -> ");
			}
			case 2 -> {
				String typeA = valueizer.apply(0);
				String typeB = valueizer.apply(1);
				sb.append("(Func_2<")
						.append(typeA).append(", ")
						.append(typeB).append(">) (")
						.append(namer.apply(0)).append(", ")
						.append(namer.apply(1)).append(") -> ");

			}
			case 3 -> {
				String typeA = valueizer.apply(0);
				String typeB = valueizer.apply(1);
				String typeC = valueizer.apply(2);
				sb.append("(Func_3<")
						.append(typeA).append(", ")
						.append(typeB).append(", ")
						.append(typeC).append(">) (")
						.append(namer.apply(0)).append(", ")
						.append(namer.apply(1)).append(", ")
						.append(namer.apply(2)).append(") -> ");
			}
			case 4 -> {
				String typeA = valueizer.apply(0);
				String typeB = valueizer.apply(1);
				String typeC = valueizer.apply(2);
				String typeD = valueizer.apply(3);
				sb.append("(Func_4<")
						.append(typeA).append(", ")
						.append(typeB).append(", ")
						.append(typeC).append(", ")
						.append(typeD).append(">) (")
						.append(namer.apply(0)).append(", ")
						.append(namer.apply(1)).append(", ")
						.append(namer.apply(2)).append(", ")
						.append(namer.apply(3)).append(") -> ");
			}
			case 5 -> {
				String typeA = valueizer.apply(0);
				String typeB = valueizer.apply(1);
				String typeC = valueizer.apply(2);
				String typeD = valueizer.apply(3);
				String typeE = valueizer.apply(4);
				sb.append("(Func_5<")
						.append(typeA).append(", ")
						.append(typeB).append(", ")
						.append(typeC).append(", ")
						.append(typeD).append(", ")
						.append(typeE).append(">) (")
						.append(namer.apply(0)).append(", ")
						.append(namer.apply(1)).append(", ")
						.append(namer.apply(2)).append(", ")
						.append(namer.apply(3)).append(", ")
						.append(namer.apply(4)).append(") -> ");
			}
			case 6 -> {
				String typeA = valueizer.apply(0);
				String typeB = valueizer.apply(1);
				String typeC = valueizer.apply(2);
				String typeD = valueizer.apply(3);
				String typeE = valueizer.apply(4);
				String typeF = valueizer.apply(5);
				sb.append("(Func_6<")
						.append(typeA).append(", ")
						.append(typeB).append(", ")
						.append(typeC).append(", ")
						.append(typeD).append(", ")
						.append(typeE).append(", ")
						.append(typeF).append(">) (")
						.append(namer.apply(0)).append(", ")
						.append(namer.apply(1)).append(", ")
						.append(namer.apply(2)).append(", ")
						.append(namer.apply(3)).append(", ")
						.append(namer.apply(4)).append(", ")
						.append(namer.apply(5)).append(") -> ");
			}
			case 7 -> {
				String typeA = valueizer.apply(0);
				String typeB = valueizer.apply(1);
				String typeC = valueizer.apply(2);
				String typeD = valueizer.apply(3);
				String typeE = valueizer.apply(4);
				String typeF = valueizer.apply(5);
				String typeG = valueizer.apply(6);
				sb.append("(Func_7<")
						.append(typeA).append(", ")
						.append(typeB).append(", ")
						.append(typeC).append(", ")
						.append(typeD).append(", ")
						.append(typeE).append(", ")
						.append(typeF).append(", ")
						.append(typeG).append(">) (")
						.append(namer.apply(0)).append(", ")
						.append(namer.apply(1)).append(", ")
						.append(namer.apply(2)).append(", ")
						.append(namer.apply(3)).append(", ")
						.append(namer.apply(4)).append(", ")
						.append(namer.apply(5)).append(", ")
						.append(namer.apply(6)).append(") -> ");
			}
		}

		// str(ctx).indexOf(i(a))
		// Math.max(i(a), i(b))
		switch (parameterCount) {
			case 0 -> {
				sb.append(wrapFunc).append("(")
						.append(methodContext).append('.').append(methodName).append("()));");
			}
			case 1 -> {
				sb.append(wrapFunc).append("(")
						.append(methodContext).append('.').append(methodName).append("(")
						.append(parameterizer.apply(0)).append("(a))));");
			}
			case 2 -> {
				sb.append(wrapFunc).append("(")
						.append(methodContext).append('.').append(methodName).append("(")
						.append(parameterizer.apply(0)).append("(a), ")
						.append(parameterizer.apply(1)).append("(b))));");

			}
			case 3 -> {
				sb.append(wrapFunc).append("(")
						.append(methodContext).append('.').append(methodName).append("(")
						.append(parameterizer.apply(0)).append("(a), ")
						.append(parameterizer.apply(1)).append("(b), ")
						.append(parameterizer.apply(2)).append("(c))));");
			}
			case 4 -> {
				sb.append(wrapFunc).append("(")
						.append(methodContext).append('.').append(methodName).append("(")
						.append(parameterizer.apply(0)).append("(a), ")
						.append(parameterizer.apply(1)).append("(b), ")
						.append(parameterizer.apply(2)).append("(c), ")
						.append(parameterizer.apply(3)).append("(d))));");
			}
			case 5 -> {
				sb.append(wrapFunc).append("(")
						.append(methodContext).append('.').append(methodName).append("(")
						.append(parameterizer.apply(0)).append("(a), ")
						.append(parameterizer.apply(1)).append("(b), ")
						.append(parameterizer.apply(2)).append("(c), ")
						.append(parameterizer.apply(3)).append("(d), ")
						.append(parameterizer.apply(4)).append("(e))));");
			}
			case 6 -> {
				sb.append(wrapFunc).append("(")
						.append(methodContext).append('.').append(methodName).append("(")
						.append(parameterizer.apply(0)).append("(a), ")
						.append(parameterizer.apply(1)).append("(b), ")
						.append(parameterizer.apply(2)).append("(c), ")
						.append(parameterizer.apply(3)).append("(d), ")
						.append(parameterizer.apply(4)).append("(e), ")
						.append(parameterizer.apply(5)).append("(f))));");
			}
			case 7 -> {
				sb.append(wrapFunc).append("(")
						.append(methodContext).append('.').append(methodName).append("(")
						.append(parameterizer.apply(0)).append("(a), ")
						.append(parameterizer.apply(1)).append("(b), ")
						.append(parameterizer.apply(2)).append("(c), ")
						.append(parameterizer.apply(3)).append("(d), ")
						.append(parameterizer.apply(4)).append("(e), ")
						.append(parameterizer.apply(5)).append("(f), ")
						.append(parameterizer.apply(6)).append("(g))));");
			}
		}
		return sb.toString();
	}
}
