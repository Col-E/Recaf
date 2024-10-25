package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import javafx.application.Platform;
import software.coley.recaf.RecafApplication;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.util.threading.ThreadUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Utility for working with JavaFX threading.
 *
 * @author Matt Coley
 * @author xDark
 * @see ThreadUtil Common threading.
 * @see ThreadPoolFactory Thread pool building.
 */
public class FxThreadUtil {
	private static final Executor jfxExecutor = FxThreadUtil::run;
	private static final List<Runnable> preInitQueue = new ArrayList<>();
	private static boolean initialized;

	/**
	 * Run action in JavaFX thread.
	 *
	 * @param action
	 * 		Runnable to start in UI thread.
	 */
	public static void run(@Nonnull Runnable action) {
		// Skip under test environment.
		if (TestEnvironment.isTestEnv()) return;

		// Hold for later if FX has not been initialized.
		if (!initialized) {
			preInitQueue.add(action);
			return;
		}

		// Wrap action so that if it fails we don't explode and kill the FX thread.
		action = ThreadUtil.wrap(action);

		// Run inline if on FX thread already, otherwise queue it up.
		if (Platform.isFxApplicationThread()) action.run();
		else Platform.runLater(action);
	}

	/**
	 * Run action in JavaFX thread.
	 *
	 * @param delayMs
	 * 		Delay to wait in milliseconds.
	 * @param action
	 * 		Runnable to start in UI thread.
	 */
	public static void delayedRun(long delayMs, @Nonnull Runnable action) {
		ThreadUtil.runDelayed(delayMs, () -> run(action));
	}

	/**
	 * @return JFX threaded executor.
	 */
	@Nonnull
	public static Executor executor() {
		return jfxExecutor;
	}

	/**
	 * Called by {@link RecafApplication} when it is first initialized.
	 */
	public static void onInitialize() {
		initialized = true;
		for (Runnable runnable : preInitQueue)
			run(runnable);
		preInitQueue.clear();
	}
}
