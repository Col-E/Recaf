package dev.xdark.recaf.jdk;

/**
 * Class loader that delegates to boot class loader.
 *
 * @author xDark
 */
public final class BootClassLoaderDelegate extends ClassLoader {

	public BootClassLoaderDelegate() {
		super(null);
	}
}
