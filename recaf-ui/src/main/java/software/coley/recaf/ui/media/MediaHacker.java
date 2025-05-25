package software.coley.recaf.ui.media;

import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmediaimpl.MediaUtils;
import jakarta.annotation.Nonnull;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.media.Media;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.util.RecafURLStreamHandlerProvider;
import software.coley.recaf.util.ReflectUtil;
import software.coley.recaf.util.UnsafeUtil;
import software.coley.recaf.workspace.model.Workspace;
import sun.misc.Unsafe;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Please look away.
 *
 * @author Matt Coley
 */
public class MediaHacker {
	/**
	 * Jesus christ, this is so fucking bad... I just want in-memory playback, oh my god.
	 *
	 * @param path
	 * 		URL to load, pointing to a {@link FileInfo} in a {@link Workspace}.
	 *
	 * @return Media instance with content loaded in memory.
	 */
	@Nonnull
	public static Media create(@Nonnull String path) throws IOException {
		// TODO: Maybe hack into JarFileFactory#fileCache and go through the regular URL constructor for Media
		//  - set the url to be cached to facilitate looking up in that cache
		//  - clear the file cache when workspace is closed
		String uriPath = RecafURLStreamHandlerProvider.fileUri(path);
		try {
			// We need to bypass the constructor due to how URI is handled normally preventing normal usage of our
			// custom URI scheme. This has caused me much pain.
			Unsafe unsafe = UnsafeUtil.get();
			Media media = (Media) unsafe.allocateInstance(Media.class);

			//  private MetadataListener metadataListener = new _MetadataListener()
			Class<?> type = Class.forName(Media.class.getName() + "$_MetadataListener");
			Constructor<?> ctor = type.getDeclaredConstructor(Media.class);
			ctor.setAccessible(true);
			Object value = ctor.newInstance(media);
			Field metadataListener = ReflectUtil.getDeclaredField(Media.class, "metadataListener");
			metadataListener.setAccessible(true);
			ReflectUtil.quietSet(media, metadataListener, value);

			//  private final ObservableMap<String,Object> metadataBacking = FXCollections.observableMap(new HashMap<String,Object>());
			Field metadataBacking = ReflectUtil.getDeclaredField(Media.class, "metadataBacking");
			metadataBacking.setAccessible(true);
			ObservableMap<Object, Object> vMetadataBacking = FXCollections.observableMap(new HashMap<>());
			ReflectUtil.quietSet(media, metadataBacking, vMetadataBacking);

			//  private final ObservableList<Track> tracksBacking = FXCollections.observableArrayList();
			Field tracksBacking = ReflectUtil.getDeclaredField(Media.class, "tracksBacking");
			tracksBacking.setAccessible(true);
			Object vTracksBacking = FXCollections.observableArrayList();
			ReflectUtil.quietSet(media, tracksBacking, vTracksBacking);

			//  private ObservableMap<String, Duration> markers = FXCollections.observableMap(new HashMap<String,Duration>());
			Field markers = ReflectUtil.getDeclaredField(Media.class, "markers");
			markers.setAccessible(true);
			ReflectUtil.quietSet(media, markers, FXCollections.observableMap(new HashMap<>()));

			// this.source = uriPath;
			Field source = ReflectUtil.getDeclaredField(Media.class, "source");
			source.setAccessible(true);
			ReflectUtil.quietSet(media, source, uriPath);

			//  metadata = FXCollections.unmodifiableObservableMap(metadataBacking);
			Field metadata = ReflectUtil.getDeclaredField(Media.class, "metadata");
			metadata.setAccessible(true);
			ReflectUtil.quietSet(media, metadata, vMetadataBacking);

			//  tracks = FXCollections.unmodifiableObservableList(tracksBacking);
			Field tracks = ReflectUtil.getDeclaredField(Media.class, "tracks");
			tracks.setAccessible(true);
			ReflectUtil.quietSet(media, tracks, vTracksBacking);

			Field jfxLocator = ReflectUtil.getDeclaredField(Media.class, "jfxLocator");
			Locator locator = new LocatorImpl(new URI(uriPath));
			ReflectUtil.quietSet(media, jfxLocator, locator);

			try {
				if (locator.canBlock()) {
					// private class InitLocator implements Runnable
					type = Class.forName(Media.class.getName() + "$InitLocator");
					ctor = type.getDeclaredConstructor(Media.class);
					ctor.setAccessible(true);
					value = ctor.newInstance(media);

					Thread t = new Thread((Runnable) value);
					t.setDaemon(true);
					t.start();
				} else {
					locator.init();
					ReflectUtil.getDeclaredMethod(Media.class, "runMetadataParser").invoke(media);
				}
			} catch (URISyntaxException ex) {
				throw new IllegalArgumentException(ex);
			} catch (FileNotFoundException ex) {
				throw new IOException("Media unavailable", ex);
			} catch (IOException ex) {
				throw new IOException("Media inaccessible", ex);
			} catch (com.sun.media.jfxmedia.MediaException ex) {
				throw new IOException("Media unsupported", ex);
			}
			return media;
		} catch (Throwable t) {
			throw new IllegalStateException(t);
		}
	}

	/**
	 * A custom locator with a modified {@link Locator#init()} that allows
	 * us to use {@link RecafURLStreamHandlerProvider}. Without this override we are limited to a few basic
	 * URI schemes such as {@code file, jar, jrt, http, https}. But since we want to keep things in memory none
	 * of these are satisfactory.
	 *
	 * @author Matt Coley
	 */
	private static class LocatorImpl extends Locator {
		public LocatorImpl(URI uri) throws URISyntaxException {
			super(uri);
			init();
		}

		@Override
		public void init() {
			try {
				// Need to decrement latch since we override the base init call
				Field fLatch = ReflectUtil.getDeclaredField(Locator.class, "readySignal");
				fLatch.setAccessible(true);
				CountDownLatch latch = ReflectUtil.quietGet(this, fLatch);
				latch.countDown();

				// Update the content type
				byte[] section = new byte[MediaUtils.MAX_FILE_SIGNATURE_LENGTH];
				InputStream stream = uri.toURL().openStream();
				if (stream.read(section) > 0)
					contentType = MediaUtils.fileSignatureToContentType(section, MediaUtils.MAX_FILE_SIGNATURE_LENGTH);

				// Cache so that the 'ConnectionHolder' uses the memory implementation, which supports seeking.
				// Without seek support 'stop' does not work.
				cacheMedia();

				// Odd note on m4a support, they rarely work if you request the player to start immediately.
				// But if you wait and then request playback it works most of the time.
			} catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

}
