package software.coley.recaf.util;

import org.slf4j.Logger;
import software.coley.recaf.analytics.SystemInformation;
import software.coley.recaf.analytics.logging.Logging;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility to patch away access restrictions.
 * <p/>
 * <b>You must initialize {@link ReflectUtil} first!</b>
 *
 * @author xDark
 */
class AccessPatcher {
	private static final Logger logger = Logging.get(AccessPatcher.class);
	private static boolean patched;

	// Deny all constructions.
	private AccessPatcher() {
	}

	/**
	 * Patches JDK access restrictions.
	 */
	static void patch() {
		if (patched) return;
		try {
			openPackages();
			patchReflectionFilters();
		} catch (Throwable t) {
			logger.error("Failed access patching on Java " + SystemInformation.JAVA_VERSION +
					"(" + SystemInformation.JAVA_VM_VENDOR + ")", t);
		} finally {
			patched = true;
		}
	}

	/**
	 * Opens all packages.
	 */
	private static void openPackages() {
		try {
			Class<?> context = AccessPatcher.class;
			Set<Module> modules = new HashSet<>();
			Module base = context.getModule();
			ModuleLayer baseLayer = base.getLayer();
			if (baseLayer != null)
				modules.addAll(baseLayer.modules());
			modules.addAll(ModuleLayer.boot().modules());
			for (ClassLoader cl = context.getClassLoader(); cl != null; cl = cl.getParent())
				modules.add(cl.getUnnamedModule());
			MethodHandle export = ReflectUtil.lookup().findVirtual(Module.class, "implAddOpens", MethodType.methodType(void.class, String.class));
			for (Module module : modules) {
				for (String name : module.getPackages()) {
					try {
						export.invokeExact(module, name);
					} catch (Exception ex) {
						logger.error("Could not export package {} in module {}", name, module);
						logger.error("Root cause: ", ex);
					}
				}
			}
		} catch (Throwable t) {
			throw new IllegalStateException("Could not export packages", t);
		}
	}

	/**
	 * Patches reflection filters.
	 */
	private static void patchReflectionFilters() {
		Class<?> klass;
		try {
			klass = Class.forName("jdk.internal.reflect.Reflection", true, null);
		} catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Unable to locate 'jdk.internal.reflect.Reflection' class", ex);
		}
		try {
			MethodHandles.Lookup lookup = ReflectUtil.lookup();
			lookup.findStaticSetter(klass, "fieldFilterMap", Map.class).invokeExact((Map<?, ?>) new HashMap<>());
			lookup.findStaticSetter(klass, "methodFilterMap", Map.class).invokeExact((Map<?, ?>) new HashMap<>());
		} catch (Throwable t) {
			throw new IllegalStateException("Unable to patch reflection filters", t);
		}
	}
}
