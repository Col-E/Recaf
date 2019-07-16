package me.coley.recaf.workspace;

import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * Importable online resource.
 *
 * @author Matt
 */
public class UrlResource extends JavaResource {
	private final URL url;
	/**
	 * Backing jar resource pointing to the local copy of the resource.
	 */
	private JavaResource backing;

	public UrlResource(URL url) {
		super(ResourceKind.URL);
		this.url = url;
		detectUrlKind();
	}

	/**
	 * @return The URL imported from.
	 */
	public URL getUrl() {
		return url;
	}

	/**
	 * Analyze the URL to determine which backing JavaResource implmentation to use.
	 */
	private void detectUrlKind() {
		String name = url.toString().toLowerCase();
		if (name.endsWith(".class")) {
			try {
				File file = File.createTempFile("recaf", "tempclass");
				FileUtils.copyURLToFile(getUrl(), file);
				backing = new ClassResource(file);
			} catch(IOException ex) {
				Logger.error(ex, "Failed to import class from URL \"{}\"", name);
			}
		} else if (name.endsWith(".jar")) {
			try {
				File file = File.createTempFile("recaf", "tempjar");
				FileUtils.copyURLToFile(getUrl(), file);
				backing = new JarResource(file);
			} catch(IOException ex) {
				Logger.error(ex, "Failed to import class from URL \"{}\"", name);
			}
		} else {
			throw new IllegalArgumentException("URLs must end in a \".class\" or \".jar\", found \"" + name + "\"");
		}
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		return backing.loadClasses();
	}

	@Override
	protected Map<String, byte[]> loadResources() throws IOException {
		return backing.loadResources();
	}

	@Override
	public String toString() {
		return url.toString();
	}
}