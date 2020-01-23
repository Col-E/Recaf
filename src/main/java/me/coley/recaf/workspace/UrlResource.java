package me.coley.recaf.workspace;

import me.coley.recaf.util.NetworkUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Importable online resource.
 *
 * @author Matt
 */
public class UrlResource extends DeferringResource {
	/**
	 * URL pointing to file.
	 */
	private final URL url;

	/**
	 * Constructs a URL resource.
	 *
	 * @param url
	 * 		The URL to pull content from. Should reference a class or jar file.
	 *
	 * @throws IOException
	 * 		When the content of the URL cannot be resolved.
	 */
	public UrlResource(URL url) throws IOException {
		super(ResourceKind.URL);
		this.url = url;
		verify();
		detectUrlKind();
	}

	/**
	 * @return The URL imported from.
	 */
	public URL getUrl() {
		return url;
	}

	/**
	 * Verify that the URL points to a valid location.
	 *
	 * @throws IOException
	 * 		When the url times out or there is no content at the URL.
	 */
	private void verify() throws IOException {
		NetworkUtil.verifyUrlContent(url);
	}

	/**
	 * Analyze the URL to determine which backing JavaResource implmentation to use.
	 */
	private void detectUrlKind() throws IOException {
		String name = url.toString().toLowerCase();
		File file;
		if (name.endsWith(".class")) {
			try {
				if (name.startsWith("file:"))
					file = new File(url.getFile());
				else {
					file = File.createTempFile("recaf", "temp.class");
					FileUtils.copyURLToFile(url, file);
				}
				file.deleteOnExit();
				setBacking(new ClassResource(file));
			} catch(IOException ex) {
				throw new IOException("Failed to import class from URL '" + name + "'", ex);
			}
		} else if (name.endsWith(".jar")) {
			try {
				if (name.startsWith("file:"))
					file = new File(url.getFile());
				else {
					file = File.createTempFile("recaf", "temp.jar");
					FileUtils.copyURLToFile(url, file);
				}
				setBacking(new JarResource(file));
			} catch(IOException ex) {
				throw new IOException("Failed to import jar from URL '" + name + "'", ex);
			}
		} else if (name.endsWith(".war")) {
			try {
				if (name.startsWith("file:"))
					file = new File(url.getFile());
				else {
					file = File.createTempFile("recaf", "temp.war");
					FileUtils.copyURLToFile(url, file);
				}
				setBacking(new WarResource(file));
			} catch(IOException ex) {
				throw new IOException("Failed to import war from URL '" + name + "'", ex);
			}
		} else {
			// Invalid URL
			throw new IOException("URLs must end in a '.class' or '.jar', found '" + name + "'");
		}
	}

	@Override
	public String toString() {
		return url.toString();
	}
}