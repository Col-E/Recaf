package software.coley.recaf.util;


import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.cdi.EagerInitialization;
import software.coley.recaf.cdi.InitializationStage;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.Workspace;

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
@ApplicationScoped
@EagerInitialization(InitializationStage.AFTER_UI_INIT)
public class RecafURLStreamHandlerProvider extends URLStreamHandlerProvider {
	private static final Logger logger = Logging.get(RecafURLStreamHandlerProvider.class);
	private static final String PREFIX = "sun.net.www.protocol";
	public static boolean installed ;
	public static final String recafClass = "rclass";
	public static final String recafFile = "rfile";
	private final WorkspaceManager workspaceManager;

	@Inject
	public RecafURLStreamHandlerProvider(@Nonnull WorkspaceManager workspaceManager) {
		this.workspaceManager = workspaceManager;
		URL.setURLStreamHandlerFactory(this);
		logger.trace("Installed Recaf URL stream handler");
		installed = true;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return URI input string in the Recaf scheme.
	 */
	@Nonnull
	public static String classUri(@Nonnull String name) {
		String path = URLEncoder.encode(name, StandardCharsets.UTF_8);
		return recafClass + ":///" + path;
	}

	/**
	 * @param name
	 * 		File name.
	 *
	 * @return URI input string in the Recaf scheme.
	 */
	@Nonnull
	public static String fileUri(@Nonnull String name) {
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
	private class ConnectionImpl extends URLConnection {
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
			if (!workspaceManager.hasCurrentWorkspace())
				throw new IOException("No workspace currently open!");

			// Transform path
			String path = getURL().getPath();
			if (path.charAt(0) == '/')
				path = path.substring(1);
			path = URLDecoder.decode(path, StandardCharsets.UTF_8);

			// Handle protocol implementations
			Workspace workspace = workspaceManager.getCurrent();
			String protocol = getURL().getProtocol();
			switch (protocol) {
				case recafClass:
					ClassPathNode classPath = workspace.findClass(path);
					if (classPath == null)
						throw new IOException("No class in current workspace: " + path);
					ClassInfo classInfo = classPath.getValue();
					content = classInfo.asJvmClass().getBytecode();
					break;
				case recafFile:
					FilePathNode filePath = workspace.findFile(path);
					if (filePath == null)
						throw new IOException("No file in current workspace: " + path);
					FileInfo fileInfo = filePath.getValue();
					content = fileInfo.getRawContent();
					break;
				default:
					throw new IOException("Unknown protocol: " + protocol);
			}
		}
	}
}