package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.FxThreadUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Coordinates script execution state across the UI.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ScriptRunController {
	private static final Logger logger = Logging.get(ScriptRunController.class);
	private final Object lock = new Object();
	private final Map<Object, ActiveExecution> activeExecutions = new HashMap<>();
	private final ReadOnlyIntegerWrapper activeRunCount = new ReadOnlyIntegerWrapper();
	/**
	 * In order to support cancellation we currently limit script execution to one at a time via the UI.
	 * Supporting concurrent runs would require each active control to own and present its own {@link GenerateResult}
	 * and completion state. If there is a demand for this later we can look into refactoring to support it.
	 */
	private final ReadOnlyBooleanWrapper running = new ReadOnlyBooleanWrapper();
	private final ReadOnlyLongWrapper executionStateVersion = new ReadOnlyLongWrapper();
	private final ScriptEngine engine;

	@Inject
	public ScriptRunController(@Nonnull ScriptEngine engine) {
		this.engine = engine;
	}

	/**
	 * @return Observable count of currently active script runs.
	 */
	@Nonnull
	public ReadOnlyIntegerProperty activeRunCountProperty() {
		return activeRunCount.getReadOnlyProperty();
	}

	/**
	 * @return Observable flag for any script execution state.
	 */
	@Nonnull
	public ReadOnlyBooleanProperty runningProperty() {
		return running.getReadOnlyProperty();
	}

	/**
	 * @return Observable version incremented whenever execution state may have changed.
	 */
	@Nonnull
	public ReadOnlyLongProperty executionStateVersionProperty() {
		return executionStateVersion.getReadOnlyProperty();
	}

	/**
	 * @return {@code true} when any script is currently compiling or running.
	 */
	public boolean isRunning() {
		synchronized (lock) {
			return !activeExecutions.isEmpty();
		}
	}

	/**
	 * @param key
	 * 		Script identity.
	 *
	 * @return {@code true} when the script identified by the key is currently compiling or running.
	 */
	public boolean isRunning(@Nonnull Object key) {
		synchronized (lock) {
			return activeExecutions.containsKey(key);
		}
	}

	/**
	 * Starts a script keyed by its source text.
	 *
	 * @param source
	 * 		Script source to compile and execute.
	 *
	 * @return Future of script execution.
	 */
	@Nonnull
	public CompletableFuture<ScriptResult> start(@Nonnull String source) {
		return start(source, source);
	}

	/**
	 * Starts a script if no other script is already active.
	 * <p>
	 * This preserves the single active cancellation handle expected by the current UI.
	 *
	 * @param key
	 * 		Script identity.
	 * @param source
	 * 		Script source to compile and execute.
	 *
	 * @return Future of script execution.
	 */
	@Nonnull
	public CompletableFuture<ScriptResult> start(@Nonnull Object key, @Nonnull String source) {
		// Setup tracking for this script execution.
		ActiveExecution execution = new ActiveExecution();
		synchronized (lock) {
			if (!activeExecutions.isEmpty())
				return CompletableFuture.failedFuture(new IllegalStateException("A script is already running"));
			activeExecutions.put(key, execution);
		}
		updateExecutionState();

		// Compile and run the script.
		engine.compile(source).whenComplete((generated, compileError) -> {
			// If compilation failed, complete the future and clear the active execution.
			if (compileError != null) {
				execution.future.completeExceptionally(compileError);
				clearActive(key, execution);
				return;
			}

			// Check if the execution was cancelled while compiling.
			boolean stopRequested;
			synchronized (lock) {
				if (activeExecutions.get(key) != execution) {
					stopRequested = true;
				} else {
					execution.generateResult = generated;
					stopRequested = execution.stopRequested;
				}
			}
			if (stopRequested)
				requestStop(generated);

			// Run the script and complete the future when done, then clear the active execution
			engine.run(generated).whenComplete((result, runError) -> {
				if (runError != null)
					execution.future.completeExceptionally(runError);
				else
					execution.future.complete(result);
				clearActive(key, execution);
			});
		});

		return execution.future;
	}

	/**
	 * Requests cancellation of all active scripts.
	 */
	public void requestStop() {
		for (ActiveExecution execution : activeExecutions()) {
			GenerateResult generated;
			synchronized (lock) {
				execution.stopRequested = true;
				generated = execution.generateResult;
			}
			if (generated != null)
				requestStop(generated);
		}
	}

	/**
	 * Requests cancellation of a script.
	 *
	 * @param key
	 * 		Script identity.
	 */
	public void requestStop(@Nonnull Object key) {
		GenerateResult generated;
		synchronized (lock) {
			ActiveExecution execution = activeExecutions.get(key);
			if (execution == null)
				return;
			execution.stopRequested = true;
			generated = execution.generateResult;
		}
		if (generated != null)
			requestStop(generated);
	}

	/**
	 * @param generated
	 * 		Script generation result to request cancellation of.
	 *
	 * @see GenerateResult#requestStop()
	 */
	private void requestStop(@Nonnull GenerateResult generated) {
		try {
			generated.requestStop();
		} catch (IllegalStateException ex) {
			logger.error("Failed to request script cancellation", ex);
		}
	}

	/**
	 * @return List of active executions.
	 */
	@Nonnull
	private List<ActiveExecution> activeExecutions() {
		synchronized (lock) {
			return new ArrayList<>(activeExecutions.values());
		}
	}

	/**
	 * Clears an execution if it is still active.
	 *
	 * @param key
	 * 		Script key.
	 * @param execution
	 * 		Active execution to clear.
	 */
	private void clearActive(@Nonnull Object key, @Nonnull ActiveExecution execution) {
		synchronized (lock) {
			if (activeExecutions.get(key) != execution)
				return;
			activeExecutions.remove(key);
		}
		updateExecutionState();
	}

	/**
	 * Updates observable execution state properties on the FX thread.
	 */
	private void updateExecutionState() {
		FxThreadUtil.run(() -> {
			int activeCount;
			synchronized (lock) {
				activeCount = activeExecutions.size();
			}
			activeRunCount.set(activeCount);
			running.set(activeCount > 0);
			executionStateVersion.set(executionStateVersion.get() + 1);
		});
	}

	/**
	 * State of an active script execution.
	 */
	private static class ActiveExecution {
		private final CompletableFuture<ScriptResult> future = new CompletableFuture<>();
		private GenerateResult generateResult;
		private boolean stopRequested;
	}
}
