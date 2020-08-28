package me.coley.recaf.util;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Bootstrap class loader for Recaf.
 *
 * @author xxDark
 */
public final class RecafClassLoader extends URLClassLoader {

    /**
     * @param urls
     *      Bootstrap URLs.
     */
    public RecafClassLoader(URL[] urls) {
        super(urls, ClassLoader.getSystemClassLoader().getParent());
    }
}
