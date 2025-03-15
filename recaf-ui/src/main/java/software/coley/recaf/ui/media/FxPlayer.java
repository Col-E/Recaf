package software.coley.recaf.ui.media;

import com.sun.media.jfxmediaimpl.NativeMediaManager;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.RecafURLStreamHandlerProvider;
import software.coley.recaf.util.ReflectUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A media-player using JavaFX's {@link MediaPlayer} with the use of {@link RecafURLStreamHandlerProvider}
 * which allows pulling audio from 'memory' via the current {@link Workspace}.
 *
 * @author Matt Coley
 */
public class FxPlayer extends Player implements AudioSpectrumListener {
	private static final Logger logger = Logging.get(FxPlayer.class);
	private final List<Runnable> playbackListeners = new ArrayList<>(2);
	private SpectrumEvent eventInstance;
	private MediaPlayer player;
	Media media;

	@Override
	public void play() {
		if (player != null) {
			player.play();
			player.setAudioSpectrumListener(this);
			playbackListeners.forEach(Runnable::run);
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
			player.setAudioSpectrumListener(null);
			playbackListeners.forEach(Runnable::run);
		}
	}

	@Override
	public void seek(double millis) {
		if (player != null) {
			player.seek(Duration.millis(millis));
			playbackListeners.forEach(Runnable::run);
		}
	}

	@Override
	public void stop() {
		if (player != null) {
			// Stop is supposed to call 'seek(0)' in implementation but for some reason it is not consistent.
			// Especially if the current state is 'paused'. If we request the seek ourselves it *seems* more reliable.
			player.seek(Duration.ZERO);
			player.stop();
			player.setAudioSpectrumListener(null);
			playbackListeners.forEach(Runnable::run);

			// Reset spectrum data
			SpectrumListener listener = getSpectrumListener();
			if (listener != null && eventInstance != null) {
				Arrays.fill(eventInstance.magnitudes(), -100);
				listener.onSpectrum(eventInstance);
			}
		}
	}

	@Override
	public void reset() {
		stop();
		media = null;
		player = null;
	}

	@Override
	public void dispose() {
		playbackListeners.clear();

		if (player != null) {
			player.dispose();
			player = null;
		}

		media = null;
	}

	@Override
	public void addPlaybackListener(Runnable r) {
		playbackListeners.add(r);
	}

	@Override
	public void load(String path) throws IOException {
		try {
			media = MediaHacker.create(path);
			player = new MediaPlayer(media);
			player.setOnError(() -> logger.warn("FX media player error"));
			player.setOnStalled(() -> logger.warn("FX media player stalled"));
			player.audioSpectrumIntervalProperty().set(0.04);
			player.setAudioSpectrumListener(this);
		} catch (Exception ex) {
			reset();
			throw new IOException("Failed to load content from: " + path, ex);
		}
	}

	@Override
	public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
		SpectrumListener listener = getSpectrumListener();
		if (listener != null) {
			if (eventInstance == null || eventInstance.magnitudes().length != magnitudes.length)
				eventInstance = new SpectrumEvent(magnitudes);
			else
				System.arraycopy(magnitudes, 0, eventInstance.magnitudes(), 0, magnitudes.length);
			listener.onSpectrum(eventInstance);
		}
		playbackListeners.forEach(Runnable::run);
	}

	@Override
	public double getMaxSeconds() {
		if (player != null)
			return player.getTotalDuration().toSeconds();
		return -1;
	}

	@Override
	public double getCurrentSeconds() {
		if (player != null)
			return player.getCurrentTime().toSeconds();
		return -1;
	}

	/**
	 * @return Current player for {@link #getMedia() media}.
	 */
	public MediaPlayer getPlayer() {
		return player;
	}

	/**
	 * @return Current loaded media.
	 */
	public Media getMedia() {
		return media;
	}

	/**
	 * @return {@code true} when there is content loaded in the player.
	 */
	public boolean hasContent() {
		return player != null;
	}

	static {
		if (!RecafURLStreamHandlerProvider.installed)
			throw new IllegalStateException("Recaf URL stream handler not installed!");
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

			// Required for newer versions of Java.
			// Ignore the compiler warning about 'invokeExact being confused' - its correct as-is.
			MethodHandle setter = ReflectUtil.lookup()
					.findStaticSetter(platformImpl, fProtocols.getName(), fProtocols.getType());
			setter.invokeExact(protocolArrayPlus);
		} catch (Throwable t) {
			throw new IllegalStateException("Could not hijack platforms to support recaf URI protocol", t);
		}
	}
}
