package me.coley.recaf.workspace;

import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.NetworkUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Importable online resource.
 *
 * @author Matt
 */
public class UrlResource extends DeferringResource {
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
	 * Analyze the URL to determine which backing JavaResource implementation to use.
	 */
	private void detectUrlKind() throws IOException {
		String name = url.toString().toLowerCase();
		Path path;
		if (name.endsWith(".class")) {
			try {
				if (name.startsWith("file:"))
					path = Paths.get(url.toURI());
				else {
					path = IOUtil.createTempFile("recaf", "temp.class");
					try (OutputStream os = Files.newOutputStream(path)) {
						IOUtil.transfer(url, os);
					}
				}
				setBacking(new ClassResource(path));
			} catch(IOException | URISyntaxException ex) {
				throw new IOException("Failed to import class from URL '" + name + "'", ex);
			}
		} else if (name.endsWith(".jar")) {
			try {
				if (name.startsWith("file:"))
					path = Paths.get(url.toURI());
				else {
					path = IOUtil.createTempFile("recaf", "temp.jar");
					try (OutputStream os = Files.newOutputStream(path)) {
						IOUtil.transfer(url, os);
					}
				}
				setBacking(new JarResource(path));
			} catch(IOException | URISyntaxException ex) {
				throw new IOException("Failed to import jar from URL '" + name + "'", ex);
			}
		} else if (name.endsWith(".war")) {
			try {
				if (name.startsWith("file:"))
					path = Paths.get(url.toURI());
				else {
					path = IOUtil.createTempFile("recaf", "temp.war");
					try (OutputStream os = Files.newOutputStream(path)) {
						IOUtil.transfer(url, os);
					}
				}
				setBacking(new WarResource(path));
			} catch(IOException | URISyntaxException ex) {
				throw new IOException("Failed to import war from URL '" + name + "'", ex);
			}
		} else {
			// Invalid URL
			throw new IOException("URLs must end in a '.class' or '.jar', found '" + name + "'");
		}
	}
}