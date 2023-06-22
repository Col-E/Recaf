package software.coley.recaf.util;

import javafx.application.Platform;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.util.threading.ThreadUtil;

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

	/**
	 * Run action in JavaFX thread.
	 *
	 * @param action
	 * 		Runnable to start in UI thread.
	 */
	public static void run(Runnable action) {
		// Skip under test environment.
		if (FxTest.isTestEnv()) return;

		// I know "Platform.isFxApplicationThread()" exists.
		// That results in some wonky behavior in various use cases though.
		Platform.runLater(ThreadUtil.wrap(action));
	}

	/**
	 * Run action in JavaFX thread.
	 *
	 * @param delayMs
	 * 		Delay to wait in milliseconds.
	 * @param action
	 * 		Runnable to start in UI thread.
	 */
	public static void delayedRun(long delayMs, Runnable action) {
		ThreadUtil.runDelayed(delayMs, () -> run(action));
	}

	/**
	 * @return JFX threaded executor.
	 */
	public static Executor executor() {
		return jfxExecutor;
	}
}
