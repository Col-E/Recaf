package me.coley.recaf.util.struct;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Dependent and non-dependent platform utilities for VM.
 *
 * @author xxDark
 */
public final class VMUtil {

    /**
     * Deny all constructions.
     */
    private VMUtil() { }

    /**
     * Appends URL to the {@link URLClassLoader}.
     *
     * @param cl  the classloader to add {@link URL} for.
     * @param url the {@link URL} to add.
     */
    public static void addURL(ClassLoader cl, URL url) {
        if (cl instanceof URLClassLoader) {
            addURL0(cl, url);
        } else {
            addURL1(cl, url);
        }
    }

    /**
     * @return running VM version.
     */
    public static int getVmVersion() {
        return (int) (Float.parseFloat(System.getProperty("java.class.version", "52" /* what? */)) - 44);
    }

    private static void addURL0(ClassLoader loader, URL url) {
        Method method;
        try {
            method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("No 'addURL' method in java.net.URLClassLoader", ex);
        }
        method.setAccessible(true);
        try {
            method.invoke(loader, url);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("'addURL' became inaccessible", ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("Error adding URL", ex.getTargetException());
        }
    }

    private static void addURL1(ClassLoader loader, URL url) {
        Class<?> currentClass =  loader.getClass();
        do {
            Field field;
            try {
                field = currentClass.getDeclaredField("ucp");
            } catch (NoSuchFieldException ignored) {
                continue;
            }
            field.setAccessible(true);
            Object ucp;
            try {
                ucp = field.get(loader);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("'ucp' became inaccessible", ex);
            }
            String className;
            if (getVmVersion() < 9) {
                className = "sun.misc.URLClassPath";
            } else {
                className = "jdk.internal.misc.URLClassPath";
            }
            Method method;
            try {
                method = Class.forName(className, true, null).getDeclaredMethod("addURL", URL.class);
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException("No 'addURL' method in " + className, ex);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(className + " was not found", ex);
            }
            method.setAccessible(true);
            try {
                method.invoke(ucp, url);
                break;
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("'addURL' became inaccessible", ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException("Error adding URL", ex.getTargetException());
            }
        } while ((currentClass=currentClass.getSuperclass()) != Object.class);
        throw new IllegalArgumentException("No 'ucp' field in " + loader);
    }

    /**
     * Closes {@link URLClassLoader}.
     *
     * @param loader
     *      Loader to close.
     *
     * @throws IOException
     *      When I/O error occurs.
     */
    public static void close(URLClassLoader loader) throws IOException {
        loader.close();
    }

    /**
     * Sets parent class loader.
     *
     * @param loader
     *      Loader to change parent for.
     * @param parent
     *      New parent loader.
     */
    public static void setParent(ClassLoader loader, ClassLoader parent) {
        Field field;
        try {
            field = ClassLoader.class.getDeclaredField("parent");
        } catch (NoSuchFieldException ex) {
            throw new IllegalStateException("No 'parent' field in java.lang.ClassLoader", ex);
        }
        field.setAccessible(true);
        try {
            field.set(loader, parent);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("'parent' became inaccessible", ex);
        }
    }

    /**
     * Initializes toolkit.
     */
    public static void tkIint() {
        if (getVmVersion() < 9) {
            PlatformImpl.startup(() -> {});
        } else {
            Method m;
            try {
                m = Platform.class.getDeclaredMethod("startup", Runnable.class);
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException("javafx.application.Platform.startup(Runnable) is missing", ex);
            }
            m.setAccessible(true);
            try {
                m.invoke(null, (Runnable) () -> {});
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("'startup' became inaccessible", ex);
            } catch (InvocationTargetException ex) {
                throw new IllegalStateException("Unable to initialize toolkit", ex.getTargetException());
            }
        }
    }
}
