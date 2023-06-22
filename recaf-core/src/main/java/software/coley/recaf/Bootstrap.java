package software.coley.recaf;

import jakarta.enterprise.inject.se.SeContainer;
import org.jboss.weld.environment.se.Weld;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.cdi.AutoRegisterWorkspaceListenersInterceptor;
import software.coley.recaf.cdi.EagerInitializationExtension;
import software.coley.recaf.cdi.WorkspaceBeanExtension;

import java.util.function.Consumer;

/**
 * Handles creation of Recaf instance.
 *
 * @author Matt Coley
 */
public class Bootstrap {
	private static final Logger logger = Logging.get(Bootstrap.class);
	private static Recaf instance;
	private static Consumer<Weld> weldConsumer;

	/**
	 * @return Recaf instance.
	 */
	public static Recaf get() {
		if (instance == null) {
			logger.info("Initializing Recaf {}", RecafBuildConfig.VERSION);
			long then = System.currentTimeMillis();

			// Create the Recaf container
			SeContainer container = createContainer();
			instance = new Recaf(container);
			logger.info("Recaf CDI container created in {}ms", System.currentTimeMillis() - then);
		}
		return instance;
	}

	/**
	 * Must be called before invoking {@link #get()}.
	 *
	 * @param consumer
	 * 		Consumer to operate on the CDI container producing {@link Weld} instance.
	 */
	public static void setWeldConsumer(Consumer<Weld> consumer) {
		weldConsumer = consumer;
	}

	private static SeContainer createContainer() {
		logger.info("Creating Recaf CDI container...");
		Weld weld = new Weld("recaf");

		// Setup custom interceptors & extensions
		logger.info("CDI: Adding interceptors & extensions");
		weld.addInterceptor(AutoRegisterWorkspaceListenersInterceptor.class);
		weld.addExtension(WorkspaceBeanExtension.getInstance());
		weld.addExtension(EagerInitializationExtension.getInstance());

		// Setup bean discovery
		//  - one instance for base package in API
		//  - one instance for base package in Core
		logger.info("CDI: Registering bean packages");
		weld.addPackage(true, RecafConstants.class);
		weld.addPackage(true, Recaf.class);

		// Handle user-defined action
		if (weldConsumer != null) {
			logger.info("CDI: Running user-defined Consumer<Weld>");
			weldConsumer.accept(weld);
			weldConsumer = null;
		}

		logger.info("CDI: Initializing...");
		return weld.initialize();
	}
}
