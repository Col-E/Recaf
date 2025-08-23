package software.coley.recaf.launch;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import software.coley.recaf.cdi.EagerInitialization;
import software.coley.recaf.cdi.InitializationStage;

/**
 * A special {@link EagerInitialization eagerly initialized bean} wrapper of {@link Runnable} which is specially by
 * the entry-point of the program. The lack of the {@link EagerInitialization} annotation on this type is intentional
 * as the entry-point determines if it should be run with {@link InitializationStage#IMMEDIATE} or
 * {@link InitializationStage#AFTER_UI_INIT} dynamically.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class LaunchHandler {
	/**
	 * Task to execute.
	 */
	public static Runnable task;

	@PostConstruct
	private void run() {
		if (task != null)
			task.run();
	}
}
