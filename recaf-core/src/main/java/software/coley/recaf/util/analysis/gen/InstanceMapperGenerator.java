package software.coley.recaf.util.analysis.gen;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.eval.InstanceFactory;
import software.coley.recaf.util.analysis.eval.InstanceMapper;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generator for items in {@link InstanceFactory} to generate {@link InstanceMapper} implementations from constructors.
 *
 * @author Matt Coley
 */
public class InstanceMapperGenerator extends GenUtils {
	static List<Class<?>> emitTargets = List.of(
			String.class,
			StringBuilder.class,
			Boolean.class,
			Byte.class,
			Character.class,
			Short.class,
			Integer.class,
			Long.class,
			Float.class,
			Double.class,
			Random.class,
			List.class,
			// Support classes
			CharSequence.class,
			ArrayList.class
	);

	/**
	 * Generator for method definitions.
	 */
	public static void main(String[] args) {
		for (Class<?> type : emitTargets) {
			System.out.println(" // " + type.getName());
			for (Constructor<?> constructor : type.getDeclaredConstructors()) {
				// Skip if we don't support a parameter type
				boolean supportedParameters = true;
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				for (Class<?> parameterType : parameterTypes) {
					if (!isSupportedType(parameterType)) {
						supportedParameters = false;
						break;
					}
				}
				if (!supportedParameters)
					continue;

				// Build final output
				String template = buildImplementation(type, constructor);
				System.out.println(" registerMapper(" + type.getSimpleName() + ".class, \"" + Type.getConstructorDescriptor(constructor) + "\", " + template + ");");
			}
			System.out.println();
		}
	}

	@Nonnull
	private static String buildImplementation(@Nonnull Class<?> type, @Nonnull Constructor<?> method) {
		return "(host, parameters) -> new " + type.getSimpleName() + "(" + buildParameterList(method.getParameterTypes()) + ")";
	}

	@Nonnull
	private static String buildParameterList(@Nonnull Class<?>[] parameterTypes) {
		//  new String(   arrc((ArrayValue) parameters.get(0))    );
		List<String> entries = new ArrayList<>(parameterTypes.length);
		for (int i = 0; i < parameterTypes.length; i++) {
			String parameter = "parameters.get(" + i + ")";
			Class<?> parameterType = parameterTypes[i];
			String entry = toMapper(parameterType) + "((" + toValue(parameterType) + ")" + parameter + ")";
			entries.add(entry);
		}
		return String.join(", ", entries);
	}
}
