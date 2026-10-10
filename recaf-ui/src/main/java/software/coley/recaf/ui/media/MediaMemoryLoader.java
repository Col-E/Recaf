package software.coley.recaf.ui.media;

import com.sun.media.jfxmedia.locator.LocatorCache;
import com.sun.media.jfxmediaimpl.MediaUtils;
import jakarta.annotation.Nonnull;
import javafx.scene.media.Media;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.util.RecafURLStreamHandlerProvider;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.net.URI;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Creates JavaFX media backed by content from the current workspace.
 *
 * @author Matt Coley
 */
public class MediaMemoryLoader {
	private static final LocatorCache CACHE = LocatorCache.locatorCache();

	/**
	 * @param path
	 * 		Path to a {@link FileInfo} in the current {@link Workspace}.
	 *
	 * @return Media instance with content loaded in memory.
	 */
	@Nonnull
	public static Media create(@Nonnull String path) throws IOException {
		if (!RecafURLStreamHandlerProvider.installed)
			throw new IllegalStateException("Recaf URL stream handler not installed!");

		// Load media bytes from the workspace into memory so that JavaFX can read it.
		URI sourceUri = URI.create(RecafURLStreamHandlerProvider.fileUri(path));
		URLConnection connection = sourceUri.toURL().openConnection();
		ByteBuffer mediaData;
		try (InputStream stream = connection.getInputStream()) {
			long contentLength = connection.getContentLengthLong();
			if (contentLength < 0 || contentLength > Integer.MAX_VALUE)
				throw new IOException("Invalid media content length: " + contentLength);

			mediaData = ByteBuffer.allocateDirect((int) contentLength);
			byte[] readBuffer = new byte[8192];
			while (mediaData.hasRemaining()) {
				int read = stream.read(readBuffer, 0, Math.min(readBuffer.length, mediaData.remaining()));
				if (read < 0)
					throw new IOException("Media content ended before its declared length");
				mediaData.put(readBuffer, 0, read);
			}
			if (stream.read() != -1)
				throw new IOException("Media content exceeded its declared length");
		}
		mediaData.flip();

		// Determine the content type of the media so that JavaFX can read it.
		String contentType = getContentType(mediaData);
		URI mediaUri = createMemoryUri(path);
		LocatorCache.CacheReference cacheReference = CACHE.registerURICache(mediaUri, mediaData, contentType);
		try {
			return new Media(mediaUri.toString());
		} catch (javafx.scene.media.MediaException ex) {
			throw new IOException("Media unsupported", ex);
		} finally {
			// LocatorCache holds weak references until Media's locator acquires its own strong reference.
			Reference.reachabilityFence(cacheReference);
		}
	}

	/**
	 * @param mediaData
	 * 		Media bytes to read the signature from.
	 *
	 * @return Content type of the media.
	 */
	@Nonnull
	private static String getContentType(@Nonnull ByteBuffer mediaData) throws IOException {
		byte[] signature = new byte[MediaUtils.MAX_FILE_SIGNATURE_LENGTH];
		ByteBuffer content = mediaData.duplicate();
		int signatureLength = Math.min(content.remaining(), signature.length);
		content.get(signature, 0, signatureLength);
		try {
			return MediaUtils.fileSignatureToContentType(signature, signature.length);
		} catch (com.sun.media.jfxmedia.MediaException ex) {
			throw new IOException("Media unsupported", ex);
		}
	}

	/**
	 * @param path
	 * 		Path to the media file in the workspace.
	 *
	 * @return URI that can be used to load the media from memory.
	 */
	@Nonnull
	private static URI createMemoryUri(@Nonnull String path) {
		int separatorIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		String fileName = path.substring(separatorIndex + 1);
		if (fileName.isEmpty())
			fileName = "media";
		String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
		return URI.create("jar:file:///recaf-media/" + UUID.randomUUID() + ".jar!/" + encodedFileName);
	}
}
