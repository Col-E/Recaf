package me.coley.recaf.util;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.scripting.impl.WorkspaceAPI;
import me.coley.recaf.workspace.Workspace;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.spi.URLStreamHandlerProvider;
import java.nio.charset.StandardCharsets;

/**
 * A hack to allow URI's to point into the current workspace.
 *
 * @author Matt Coley
 */
public class RecafURLStreamHandlerProvider extends URLStreamHandlerProvider {
	private static final String PREFIX = "sun.net.www.protocol";
	private static final RecafURLStreamHandlerProvider instance = new RecafURLStreamHandlerProvider();
	public static final String recafClass = "rclass";
	public static final String recafFile = "rfile";
	private static boolean installed;

	private RecafURLStreamHandlerProvider() {
		// deny construction
	}

	/**
	 * Installs the stream handler.
	 */
	public static void install() {
		if (!installed) {
			URL.setURLStreamHandlerFactory(instance);
			installed = true;
		}
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return URI input string in the Recaf scheme.
	 */
	public static String classUri(String name) {
		String path = URLEncoder.encode(name, StandardCharsets.UTF_8);
		return recafClass + ":///" + path;
	}

	/**
	 * @param name
	 * 		File name.
	 *
	 * @return URI input string in the Recaf scheme.
	 */
	public static String fileUri(String name) {
		String path = URLEncoder.encode(name, StandardCharsets.UTF_8);
		return recafFile + ":///" + path;
	}

	@Override
	@SuppressWarnings("deprecation")
	public URLStreamHandler createURLStreamHandler(String protocol) {
		if (protocol.equals(recafClass) || protocol.equals(recafFile))
			return new URLStreamHandler() {
				@Override
				protected URLConnection openConnection(URL url) {
					return new ConnectionImpl(url);
				}
			};
		// Default implementation (See implementation in URL class)
		String name = PREFIX + "." + protocol + ".Handler";
		try {
			return (URLStreamHandler) Class.forName(name).newInstance();
		} catch (Exception ignored) {
			// Ignored
		}
		return null;
	}

	/**
	 * Connection impl to actually pull from the current workspace.
	 */
	private static class ConnectionImpl extends URLConnection {
		private byte[] content;

		public ConnectionImpl(URL url) {
			super(url);
		}

		@Override
		public void connect() {
			// no-op
		}

		@Override
		public InputStream getInputStream() throws IOException {
			if (content == null)
				loadContent();
			return new ByteArrayInputStream(content);
		}


		@Override
		public String getContentType() {
			return guessContentTypeFromName(url.getFile());
		}

		@Override
		public long getContentLengthLong() {
			return getContentLength();
		}

		@Override
		public int getContentLength() {
			if (content == null)
				try {
					loadContent();
				} catch (IOException ex) {
					return -1;
				}
			return content.length;
		}

		private void loadContent() throws IOException {
			// Validate state
			Workspace workspace = WorkspaceAPI.getWorkspace();
			if (workspace == null)
				throw new IOException("No workspace currently open!");
			// Transform path
			String path = getURL().getPath();
			if (path.charAt(0) == '/')
				path = path.substring(1);
			path = URLDecoder.decode(path, StandardCharsets.UTF_8);
			// Handle protocol implementations
			String protocol = getURL().getProtocol();
			switch (protocol) {
				case recafClass:
					ClassInfo classInfo = workspace.getResources().getClass(path);
					if (classInfo == null)
						throw new IOException("No class in current workspace: " + path);
					content = classInfo.getValue();
					break;
				case recafFile:
					FileInfo fileInfo = workspace.getResources().getFile(path);
					if (fileInfo == null)
						throw new IOException("No file in current workspace: " + path);
					content = fileInfo.getValue();
					break;
				default:
					throw new IOException("Unknown protocol: " + protocol);
			}
		}
	}
}
