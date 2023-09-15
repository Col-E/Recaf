package software.coley.recaf.util;

import org.slf4j.Logger;
import software.coley.recaf.analytics.SystemInformation;
import software.coley.recaf.analytics.logging.Logging;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;

import static software.coley.recaf.util.JigsawUtil.*;

/**
 * Utility to patch away access restrictions.
 *
 * @author xDark
 */
public class AccessPatcher {
	private static final Logger logger = Logging.get(AccessPatcher.class);
	private static boolean patched;

	// Deny all constructions.
	private AccessPatcher() {
	}

	/**
	 * Patches JDK access restrictions.
	 */
	public static void patch() {
		if (patched) return;
		try {
			logger.debug("Opening access to all packages");
			openPackages();
			logger.debug("Patching package reflection restrictions");
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
			Method export = Module.class.getDeclaredMethod("implAddOpens", String.class);
			setMethodModifiers(export, Modifier.PUBLIC);
			HashSet<Module> modules = new HashSet<>();
			Class<?> classBase = JigsawUtil.class;
			Module base = getClassModule(classBase);
			if (base.getLayer() != null)
				modules.addAll(base.getLayer().modules());
			modules.addAll(ModuleLayer.boot().modules());
			for (ClassLoader cl = classBase.getClassLoader(); cl != null; cl = cl.getParent()) {
				modules.add(getLoaderModule(cl));
			}
			for (Module module : modules) {
				for (String name : module.getPackages()) {
					try {
						export.invoke(module, name);
					} catch (Exception ex) {
						logger.error("Could not export package {} in module {}", name, module);
						logger.error("Root cause: ", ex);
					}
				}
			}
		} catch (Exception ex) {
			throw new IllegalStateException("Could not export packages", ex);
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
			throw new RuntimeException("Unable to locate 'jdk.internal.reflect.Reflection' class", ex);
		}
		try {
			Field[] fields;
			try {
				Method m = Class.class.getDeclaredMethod("getDeclaredFieldsImpl");
				m.setAccessible(true);
				fields = (Field[]) m.invoke(klass);
			} catch (NoSuchMethodException | InvocationTargetException ex) {
				try {
					Method m = Class.class.getDeclaredMethod("getDeclaredFields0", Boolean.TYPE);
					m.setAccessible(true);
					fields = (Field[]) m.invoke(klass, false);
				} catch (InvocationTargetException | NoSuchMethodException ex1) {
					ex.addSuppressed(ex1);
					throw new RuntimeException("Unable to get all class fields", ex);
				}
			}
			int c = 0;
			for (Field field : fields) {
				String name = field.getName();
				if ("fieldFilterMap".equals(name) || "methodFilterMap".equals(name)) {
					field.setAccessible(true);
					field.set(null, new HashMap<>(0));
					if (++c == 2) {
						return;
					}
				}
			}
			throw new RuntimeException("One of field patches did not apply properly. " +
					"Expected to patch two fields, but patched: " + c);
		} catch (IllegalAccessException ex) {
			throw new RuntimeException("Unable to patch reflection filters", ex);
		}
	}
}
