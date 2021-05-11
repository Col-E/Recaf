package me.coley.recaf.presentation;

import me.coley.recaf.Controller;
import me.coley.recaf.RecafConstants;
import me.coley.recaf.RecafUI;
import me.coley.recaf.config.ConfigRegistry;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.util.JFXUtils;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.util.JFXInjection;
import me.coley.recaf.util.LoggerConsumerImpl;
import me.coley.recaf.util.Threads;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

/**
 * JavaFX based GUI presentation.
 *
 * @author Matt Coley
 */
public class GuiPresentation implements Presentation {
	private static final Logger logger = Logging.get(GuiPresentation.class);
	private Controller controller;

	@Override
	public void initialize(Controller controller) {
		this.controller = controller;
		// Setup logging
		Logging.addLogConsumer(new LoggerConsumerImpl());
		// Setup JavaFX
		JFXInjection.ensureJavafxSupport();
		JFXUtils.initializePlatform();
		// Load translations
		Lang.initialize();
		// Setup config
		Configs.containers().forEach(ConfigRegistry::register);
		ConfigRegistry.load();
		// Open UI
		JFXUtils.runSafe(() -> {
			try {
				RecafUI.initialize(controller);
				RecafUI.getWindows().getMainWindow().show();
			} catch (Throwable ex) {
				logger.error("Recaf crashed due to an unhandled error." +
						"Please open a bug report: " + RecafConstants.URL_BUG_REPORT, ex);
				JFXUtils.shutdownSafe();
				System.exit(-1);
			}
		});
		// Intercept / log uncaught exceptions
		Thread.UncaughtExceptionHandler exceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
			logger.error("Uncaught exception on thread '" + thread.getName() + "', error={}", exception);
			if (exceptionHandler != null)
				exceptionHandler.uncaughtException(thread, exception);
		});
		// Shutdown handler
		Runtime.getRuntime().addShutdownHook(new Thread(Threads::shutdown));
	}

	@Override
	public WorkspacePresentation workspaceLayer() {
		return new GuiWorkspacePresentation(controller);
	}
}
