package software.coley.recaf.util;

import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Package-private util to deal with modules.
 *
 * @author xDark
 */
@SuppressWarnings("sunapi")
public class JigsawUtil {
	private static final Logger logger = Logging.get(JigsawUtil.class);
	private static final MethodHandle CLASS_MODULE;
	private static final MethodHandle CLASS_LOADER_MODULE;
	private static final MethodHandle METHOD_MODIFIERS;
	private static final Lookup lookup;

	// Deny all constructions.
	private JigsawUtil() {
	}

	/**
	 * @return {@code IMPL_LOOKUP} from {@link MethodHandles.Lookup}.
	 */
	public static Lookup getLookup() {
		return lookup;
	}

	/**
	 * @param klass
	 *        {@link Class} to get module from.
	 *
	 * @return {@link Module} of the class.
	 */
	static Module getClassModule(Class<?> klass) {
		try {
			return (Module) CLASS_MODULE.invokeExact(klass);
		} catch (Throwable t) {
			logger.error("Failed getting class module: " + klass.getName(), t);
			// That should never happen.
			throw new AssertionError(t);
		}
	}

	/**
	 * @param loader
	 *        {@link ClassLoader} to get module from.
	 *
	 * @return {@link Module} of the class.
	 */
	static Module getLoaderModule(ClassLoader loader) {
		try {
			return (Module) CLASS_LOADER_MODULE.invokeExact(loader);
		} catch (Throwable t) {
			logger.error("Failed getting ClassLoader module: " + loader, t);
			// That should never happen.
			throw new AssertionError(t);
		}
	}

	/**
	 * @param method
	 *        {@link Method} to change modifiers for.
	 * @param modifiers
	 * 		new modifiers.
	 */
	static void setMethodModifiers(Method method, int modifiers) {
		try {
			METHOD_MODIFIERS.invokeExact(method, modifiers);
		} catch (Throwable t) {
			logger.error("Failed setting method modifiers: " + method.getName(), t);
			// That should never happen.
			throw new AssertionError(t);
		}
	}

	static {
		try {
			Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
			Field field = ReflectUtil.getDeclaredField(unsafeClass, "theUnsafe");
			Object unsafe = field.get(null);
			field = Lookup.class.getDeclaredField("IMPL_LOOKUP");
			MethodHandles.publicLookup();
			Object base = ReflectUtil.quietInvoke(unsafeClass, unsafe, "staticFieldBase",
					new Class[]{Field.class}, new Object[]{field});
			long offset = ReflectUtil.quietInvoke(unsafeClass, unsafe, "staticFieldOffset",
					new Class[]{Field.class}, new Object[]{field});
			lookup = ReflectUtil.quietInvoke(unsafeClass, unsafe, "getObject",
					new Class[]{Object.class, Long.TYPE},
					new Object[]{base, offset});
			MethodType type = MethodType.methodType(Module.class);
			CLASS_MODULE = lookup.findVirtual(Class.class, "getModule", type);
			CLASS_LOADER_MODULE = lookup.findVirtual(ClassLoader.class, "getUnnamedModule", type);
			METHOD_MODIFIERS = lookup.findSetter(Method.class, "modifiers", Integer.TYPE);
		} catch (ClassNotFoundException | NoSuchMethodException
				 | IllegalAccessException | NoSuchFieldException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}
}
