package software.coley.recaf.util.analysis.gen;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.analysis.eval.InstanceFactory;
import software.coley.recaf.util.analysis.eval.MethodInvokeHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Generator for items in {@link InstanceFactory} to generate {@link MethodInvokeHandler} implementations.
 *
 * @author Matt Coley
 */
public class InstanceMethodInvokeHandlerGenerator extends GenUtils {
	/**
	 * Generator for method definitions.
	 */
	public static void main(String[] args) {
		for (Class<?> type : InstanceMapperGenerator.emitTargets) {
			System.out.println(" // " + type.getName());
			for (Method method : type.getDeclaredMethods()) {
				// Skip inaccessible methods
				int modifiers = method.getModifiers();
				if (!Modifier.isPublic(modifiers))
					continue;
				if (Modifier.isStatic(modifiers))
					continue;
				if (AccessFlag.isBridge(modifiers) || AccessFlag.isSynthetic(modifiers))
					continue;

				// Skip static initializer and constructors
				String methodName = method.getName();
				if (method.getName().charAt(0) == '<')
					continue;

				// Skip if we don't support the return type
				if (!isSupportedTypeOrEmitTarget(method.getReturnType()))
					continue;

				// Skip if we don't support a parameter type
				boolean supportedParameters = true;
				Class<?>[] parameterTypes = method.getParameterTypes();
				for (Class<?> parameterType : parameterTypes) {
					if (!isSupportedTypeOrEmitTarget(parameterType)) {
						supportedParameters = false;
						break;
					}
				}
				if (!supportedParameters)
					continue;

				// Build final output
				String methodDescriptor = Type.getMethodDescriptor(method);
				String template = buildImplementation(type, method);
				System.out.println(" registerMethodHandler(\"" + type.getName().replace('.', '/') +
						"\", \"" + methodName + "\", \"" + methodDescriptor + "\", " + template + "  );");
			}
			System.out.println();
		}
	}

	@Nonnull
	private static String buildImplementation(@Nonnull Class<?> type, @Nonnull Method method) {
		//  ReValue host, T receiver, List<ReValue> args -> new InstancedObjectValue<>(receiver.method( %MAPPER%(args.get(0)), ... ))
		//                   for primitive methods       -> %MAPPER%(receiver.method( %MAPPER%(args.get(0)), ... ))
		//                        for void methods       -> { receiver.method( %MAPPER%(args.get(0)), ... ); return null; }
		//                            for builders       -> { receiver.method( %MAPPER%(args.get(0)), ... ); return host; }
		StringBuilder sb = new StringBuilder("(ReValue host, " + type.getSimpleName() + " receiver, List<ReValue> args) -> ");
		Class<?> returnType = method.getReturnType();
		boolean isBuilder = (type == StringBuffer.class || type == StringBuilder.class) && returnType == type;
		boolean isPrimitive = returnType.isPrimitive() || returnType == String.class || returnType == CharSequence.class;
		if (returnType == void.class || isBuilder) {
			sb.append("{ receiver.").append(method.getName()).append("(");
		} else if (isPrimitive) {
			sb.append(toMapper(returnType)).append("(receiver.").append(method.getName()).append("(");
		} else {
			sb.append("new InstancedObjectValue<>(receiver.").append(method.getName()).append("(");
		}

		Class<?>[] parameterTypes = method.getParameterTypes();
		for (int i = 0; i < parameterTypes.length; i++) {
			Class<?> parameterType = parameterTypes[i];
			String entry = toMapper(parameterType) + "((" + toValue(parameterType) + ")" + "args.get(" + i + "))";
			sb.append(entry);
			if (i < parameterTypes.length - 1)
				sb.append(", ");
		}
		sb.append(")");
		if (returnType == void.class) {
			sb.append("; return null; }");
		} else if (isBuilder) {
			sb.append("; return host; }");
		} else {
			sb.append(")");
		}

		String string = sb.toString();
		if (StringUtil.count('(', string) != StringUtil.count(')', string))
			throw new IllegalStateException("Mismatched parentheses in generated code: " + string);
		return string;
	}

	private static boolean isSupportedTypeOrEmitTarget(@Nonnull Class<?> cls) {
		return isSupportedType(cls) || cls == void.class || InstanceMapperGenerator.emitTargets.contains(cls);
	}
}
