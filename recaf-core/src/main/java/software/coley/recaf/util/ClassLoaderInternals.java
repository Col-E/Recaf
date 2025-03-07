package software.coley.recaf.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * Hacky code to get internal classloader state.
 *
 * @author xDark
 * @author Matt Coley
 */
public class ClassLoaderInternals {
	/**
	 * @return {@code jdk.internal.loader.URLClassPath} instance.
	 *
	 * @throws ReflectiveOperationException
	 * 		When the internals change and the reflective look-ups fail.
	 */
	public static Object getUcp() throws ReflectiveOperationException {
		// Fetch UCP of application's ClassLoader
		// - ((ClassLoaders.AppClassLoader) ClassLoaders.appClassLoader()).ucp
		Class<?> clsClassLoaders = Class.forName("jdk.internal.loader.ClassLoaders");
		Object appClassLoader = ReflectUtil.quietInvoke(clsClassLoaders, null, "appClassLoader",
				new Class[0], new Object[0]);
		Class<?> ucpOwner = appClassLoader.getClass();

		// Field removed in 16, but still exists in parent class "BuiltinClassLoader"
		if (JavaVersion.get() >= 16)
			ucpOwner = ucpOwner.getSuperclass();

		Field fieldUCP = ReflectUtil.getDeclaredField(ucpOwner, "ucp");
		return fieldUCP.get(appClassLoader);
	}

	/**
	 * @param ucp
	 * 		See {@link #getUcp()}.
	 *
	 * @return The contents of the UCP.
	 *
	 * @throws ReflectiveOperationException
	 * 		When the internals change and the reflective look-ups fail.
	 */
	@SuppressWarnings("unchecked")
	public static List<URL> getUcpPathList(Object ucp) throws ReflectiveOperationException {
		if (ucp == null)
			return Collections.emptyList();
		Class<?> ucpClass = ucp.getClass();
		Field path = ReflectUtil.getDeclaredField(ucpClass, "path");
		return (List<URL>) path.get(ucp);
	}

	/**
	 * @param ucp
	 * 		See {@link #getUcp()}.
	 * @param url
	 * 		URL to add to the search path for directories and Jar files of the UCP.
	 *
	 * @throws ReflectiveOperationException
	 * 		When the internals change and the reflective look-ups fail.
	 */
	public static void appendToUcpPath(Object ucp, URL url) throws ReflectiveOperationException {
		if (ucp == null)
			return;
		Class<?> ucpClass = ucp.getClass();
		Method addURL = ReflectUtil.getDeclaredMethod(ucpClass, "addURL", URL.class);
		addURL.invoke(ucp, url);
	}
}