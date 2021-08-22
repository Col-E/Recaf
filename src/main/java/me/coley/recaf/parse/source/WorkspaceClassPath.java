package me.coley.recaf.parse.source;

import javassist.ClassPath;
import me.coley.recaf.util.Log;
import me.coley.recaf.workspace.Workspace;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Javassist classpath implementation that pulls classes from a workspace.
 *
 * @author Matt Coley
 */
public class WorkspaceClassPath implements ClassPath {
	private static final RecafStreamHandler dummyUrlHandler = new RecafStreamHandler();
	private final Workspace workspace;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public WorkspaceClassPath(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public InputStream openClassfile(String classname) {
		byte[] bytes = workspace.getRawClass(classname);
		if (bytes != null)
			return new ByteArrayInputStream(bytes);
		return null;
	}

	@Override
	public URL find(String classname) {
		// Javassist does a URL lookup before allowing any actual reading, so we have to supply some
		// dummy value in order to then call "openClassfile"
		try {
			String internalName = classname.replace('.', '/');
			return new URL(null, "recaf:" + internalName, dummyUrlHandler);
		} catch (Exception ex) {
			Log.error("Failed to create 'recaf' URL scheme", ex);
			return null;
		}
	}

	private static class RecafStreamHandler extends URLStreamHandler {
		@Override
		protected URLConnection openConnection(URL u) {
			// No-op
			return null;
		}
	}
}
