package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import software.coley.recaf.RecafApplication;
import software.coley.recaf.util.threading.Batch;
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
	 * Assign a property value on the FX thread.
	 *
	 * @param property
	 * 		Property to update.
	 * @param value
	 * 		Value to assign to the property.
	 * @param <T>
	 * 		Property value type.
	 */
	public static <T> void set(@Nonnull WritableValue<T> property, @Nullable T value) {
		run(() -> property.setValue(value));
	}

	/**
	 * Binds a property value on the FX thread.
	 *
	 * @param property
	 * 		Property to bind.
	 * @param value
	 * 		Value to bind the property to.
	 * @param <T>
	 * 		Property value type.
	 */
	public static <T> void bind(@Nonnull Property<T> property, @Nullable ObservableValue<T> value) {
		// Binding will fire off event handlers if the given value does not match the current property value.
		// When this is done off the FX thread it can be unsafe.
		run(() -> property.bind(value));
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

	/**
	 * @return New execution chain.
	 */
	@Nonnull
	public static Batch batch() {
		return new FxBatch();
	}

	/**
	 * Batch implementation that executes all tasks on the FX thread.
	 */
	private static class FxBatch implements Batch {
		private final List<Runnable> tasks = new ArrayList<>();

		@Override
		public void add(@Nonnull Runnable runnable) {
			synchronized (tasks) {
				tasks.add(runnable);
			}
		}

		@Override
		public void clear() {
			synchronized (tasks) {
				tasks.clear();
			}
		}

		@Override
		public void execute() {
			run(this::fire);
		}

		private void fire() {
			synchronized (tasks) {
				for (Runnable task : tasks)
					task.run();
				tasks.clear();
			}
		}
	}
}
