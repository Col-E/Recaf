package me.coley.recaf.ui.media;

import com.sun.media.jfxmedia.MediaManager;
import com.sun.media.jfxmedia.MediaPlayer;
import com.sun.media.jfxmedia.events.AudioSpectrumEvent;
import com.sun.media.jfxmedia.events.AudioSpectrumListener;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmediaimpl.MediaUtils;
import com.sun.media.jfxmediaimpl.NativeMediaManager;
import com.sun.media.jfxmediaimpl.NativeMediaPlayer;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import me.coley.recaf.util.JigsawUtil;
import me.coley.recaf.util.RecafURLStreamHandlerProvider;
import me.coley.recaf.util.ReflectUtil;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * An audio-player using JavaFX's {@link MediaPlayer} with the use of {@link RecafURLStreamHandlerProvider}
 * which allows pulling audio from 'memory' via the current {@link me.coley.recaf.workspace.Workspace}.
 *
 * @author Matt Coley
 */
public class MemoryPlayer implements AudioSpectrumListener {
	private final SimpleObjectProperty<AudioSpectrumEvent> lastSpectrum = new SimpleObjectProperty<>();
	private Locator locator;
	private MediaPlayer player;

	/**
	 * Play the track.
	 */
	public void play() {
		if (player != null) {
			player.play();
			player.addAudioSpectrumListener(this);
		}
	}

	/**
	 * Pause the track.
	 */
	public void pause() {
		if (player != null) {
			player.pause();
			player.removeAudioSpectrumListener(this);
		}
	}

	/**
	 * Stop the track.
	 */
	public void stop() {
		if (player != null) {
			player.stop();
			player.removeAudioSpectrumListener(this);
		}
	}

	/**
	 * Stop the track and clear references to the player.
	 */
	public void reset() {
		stop();
		locator = null;
		player = null;
	}

	/**
	 * Initialize a new track.
	 *
	 * @param path
	 * 		File path name in the current {@link me.coley.recaf.workspace.Workspace}.
	 *
	 * @throws IOException
	 * 		When the file cannot be loaded for playback.
	 */
	public void load(String path) throws IOException {
		String uriPath = RecafURLStreamHandlerProvider.fileUri(path);
		try {
			URI uri = new URI(uriPath);
			locator = new LocatorImpl(uri);
			player = MediaManager.getPlayer(locator);
			player.getAudioSpectrum().setInterval(0.04);
			player.getAudioSpectrum().setEnabled(true);
		} catch (Exception ex) {
			reset();
			throw new IOException("Failed to load content from: " + path, ex);
		}
	}

	/**
	 * @return Property wrapper of the last spectrum event.
	 */
	public Property<AudioSpectrumEvent> lastSpectrumProperty() {
		return lastSpectrum;
	}

	/**
	 * @return {@code true} when there is content loaded in the player.
	 */
	public boolean hasContent() {
		return player != null;
	}

	/**
	 * @return Content type name of the audio format being played.
	 */
	public String getContentType() {
		return locator.getContentType();
	}

	/**
	 * Used to prevent removal of the weak reference to {@link AudioSpectrumListener} in {@link NativeMediaPlayer}.
	 */
	public void iAmStillBeingUsedPleaseDoNotPurgeMyWeakReference() {
		// This is a really stupid hack to ensure the weak-reference of the spectrum-listeners
		// isn't invalidated, and we suddenly lose all of our spectrum update event notifications.
	}

	@Override
	public void onAudioSpectrumEvent(AudioSpectrumEvent evt) {
		// TODO: Inline this, and create a listener interface
		//  - prevent user of this class from having to call the cringe method
		//  - maybe abstract away JFX usage so we can also use javax.sound/javazoom support?
		lastSpectrum.set(evt);
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
			} catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	static {
		RecafURLStreamHandlerProvider.install();
		try {
			// Inject protocol name in manager
			NativeMediaManager manager = NativeMediaManager.getDefaultInstance();
			Field fProtocols = ReflectUtil.getDeclaredField(NativeMediaManager.class, "supportedProtocols");
			fProtocols.setAccessible(true);
			List<String> protocols = ReflectUtil.quietGet(manager, fProtocols);
			protocols.add(RecafURLStreamHandlerProvider.recafClass);
			protocols.add(RecafURLStreamHandlerProvider.recafFile);
			// Inject protocol name into platform impl
			Class<?> platformImpl = Class.forName("com.sun.media.jfxmediaimpl.platform.gstreamer.GSTPlatform");
			fProtocols = platformImpl.getDeclaredField("PROTOCOLS");
			fProtocols.setAccessible(true);
			String[] protocolArray = (String[]) fProtocols.get(null);
			String[] protocolArrayPlus = new String[protocolArray.length + 1];
			System.arraycopy(protocolArray, 0, protocolArrayPlus, 0, protocolArray.length);
			protocolArrayPlus[protocolArray.length] = RecafURLStreamHandlerProvider.recafFile;
			JigsawUtil.getLookup().unreflectSetter(fProtocols)
					.invoke((Object) protocolArrayPlus);
		} catch (Throwable t) {
			throw new IllegalStateException("Could not hijack platforms to support recaf URI protocol", t);
		}
	}
}
