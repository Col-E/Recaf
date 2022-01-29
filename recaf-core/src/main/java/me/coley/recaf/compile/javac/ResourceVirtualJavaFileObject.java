package me.coley.recaf.compile.javac;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Java file extension that exposes
 * workspace resource for classpath.
 *
 * @author xDark
 */
public class ResourceVirtualJavaFileObject extends SimpleJavaFileObject {

	/**
	 * Resource name
	 */
	private final String resourceName;

	/**
	 * Resource content.
	 */
	private final byte[] content;

	/**
	 * @param resourceName
	 * 		Name of the resource.
	 * @param content
	 * 		Class source content.
	 * @param resourceKind
	 * 		Kind of the resource.
	 */
	public ResourceVirtualJavaFileObject(String resourceName, byte[] content, Kind resourceKind) {
		super(URI.create("memory://" + resourceName + resourceKind.extension),
				resourceKind);
		this.resourceName = resourceName;
		this.content = content;
	}

	/**
	 * Returns resource name.
	 *
	 * @return resource name.
	 */
	public String getResourceName() {
		return resourceName;
	}

	@Override
	public InputStream openInputStream() throws IOException {
		return new ByteArrayInputStream(content);
	}
}