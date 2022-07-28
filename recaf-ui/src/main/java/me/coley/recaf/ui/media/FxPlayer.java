package me.coley.recaf.ui.media;

import com.sun.media.jfxmedia.MediaManager;
import com.sun.media.jfxmedia.MediaPlayer;
import com.sun.media.jfxmedia.effects.AudioSpectrum;
import com.sun.media.jfxmedia.events.AudioSpectrumEvent;
import com.sun.media.jfxmedia.events.AudioSpectrumListener;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmediaimpl.MediaUtils;
import com.sun.media.jfxmediaimpl.NativeMediaManager;
import dev.xdark.ssvm.util.UnsafeUtil;
import me.coley.recaf.util.RecafURLStreamHandlerProvider;
import me.coley.recaf.util.ReflectUtil;
import sun.misc.Unsafe;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * An audio-player using JavaFX's {@link MediaPlayer} with the use of {@link RecafURLStreamHandlerProvider}
 * which allows pulling audio from 'memory' via the current {@link me.coley.recaf.workspace.Workspace}.
 *
 * @author Matt Coley
 */
public class FxPlayer extends AudioPlayer implements AudioSpectrumListener {
	private static final float[] EMPTY_ARRAY = new float[0];
	private SpectrumEvent eventInstance;
	private Locator locator;
	private MediaPlayer player;

	@Override
	public void play() {
		if (player != null) {
			player.play();
			player.addAudioSpectrumListener(this);
		}
	}

	@Override
	public void pause() {
		// TODO: Pausing and then unpausing sometimes causes the spectrum listener to feel 'laggy'
		//  - but the spectrum interval is still the same so its not like that is getting reset
		//  - happens more consistently with mp3 files, m4a is seemingly unaffected
		//  - and pausing/unpausing can sometimes also fix the 'laggy' feeling.
		if (player != null) {
			player.pause();
			player.removeAudioSpectrumListener(this);
		}
	}

	@Override
	public void stop() {
		if (player != null) {
			// Stop is supposed to call 'seek(0)' in implementation but for some reason it is not consistent.
			// Especially if the current state is 'paused'. If we request the seek ourselves it *seems* more reliable.
			player.seek(0);
			player.stop();
			player.removeAudioSpectrumListener(this);
			// Reset spectrum data
			SpectrumListener listener = getSpectrumListener();
			if (listener != null && eventInstance != null) {
				Arrays.fill(eventInstance.getMagnitudes(), -100);
				listener.onSpectrum(eventInstance);
			}
		}
	}

	@Override
	public void reset() {
		stop();
		locator = null;
		player = null;
	}

	@Override
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

	@Override
	public void onAudioSpectrumEvent(AudioSpectrumEvent evt) {
		SpectrumListener listener = getSpectrumListener();
		if (listener != null) {
			AudioSpectrum source = evt.getSource();
			float[] magnitudes = source.getMagnitudes(EMPTY_ARRAY);
			if (eventInstance == null || eventInstance.getMagnitudes().length != magnitudes.length)
				eventInstance = new SpectrumEvent(magnitudes);
			else
				System.arraycopy(magnitudes, 0, eventInstance.getMagnitudes(), 0, magnitudes.length);
			listener.onSpectrum(eventInstance);
		}
	}

	@Override
	public double getMaxSeconds() {
		if (player != null)
			return player.getDuration();
		return -1;
	}

	@Override
	public double getCurrentSeconds() {
		if (player != null)
			return player.getPresentationTime();
		return -1;
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

	static {
		RecafURLStreamHandlerProvider.install();
		try {
			// Inject protocol name in manager
			NativeMediaManager manager = NativeMediaManager.getDefaultInstance();
			Field fProtocols = ReflectUtil.getDeclaredField(NativeMediaManager.class, "supportedProtocols");
			fProtocols.setAccessible(true);
			List<String> protocols = ReflectUtil.quietGet(manager, fProtocols);
			protocols.add(RecafURLStreamHandlerProvider.recafFile);
			// Inject protocol name into platform impl
			Class<?> platformImpl = Class.forName("com.sun.media.jfxmediaimpl.platform.gstreamer.GSTPlatform");
			fProtocols = platformImpl.getDeclaredField("PROTOCOLS");
			fProtocols.setAccessible(true);
			String[] protocolArray = (String[]) fProtocols.get(null);
			String[] protocolArrayPlus = new String[protocolArray.length + 1];
			System.arraycopy(protocolArray, 0, protocolArrayPlus, 0, protocolArray.length);
			protocolArrayPlus[protocolArray.length] = RecafURLStreamHandlerProvider.recafFile;
			// Required for newer versions of Java
			Unsafe unsafe = UnsafeUtil.get();
			Object fieldBase = unsafe.staticFieldBase(fProtocols);
			long fieldOffset = unsafe.staticFieldOffset(fProtocols);
			unsafe.putObject(fieldBase, fieldOffset, protocolArrayPlus);
		} catch (Throwable t) {
			throw new IllegalStateException("Could not hijack platforms to support recaf URI protocol", t);
		}
	}
}
