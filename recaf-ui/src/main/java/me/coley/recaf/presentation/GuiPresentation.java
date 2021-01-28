package me.coley.recaf.presentation;

import me.coley.recaf.Controller;
import me.coley.recaf.RecafConstants;
import me.coley.recaf.RecafUI;
import me.coley.recaf.util.JFXInjection;
import me.coley.recaf.util.JFXUtils;
import me.coley.recaf.util.LoggerConsumerImpl;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

/**
 * JavaFX based GUI presentation.
 *
 * @author Matt Coley
 */
public class GuiPresentation implements Presentation {
	private static final Logger logger = Logging.get(GuiPresentation.class);

	@Override
	public void initialize(Controller controller) {
		// Setup logging
		Logging.addLogConsumer(new LoggerConsumerImpl());
		// Setup JavaFX
		JFXInjection.ensureJavafxSupport();
		JFXUtils.initializePlatform();
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
	}

	@Override
	public WorkspacePresentation workspaceLayer() {
		// TODO
		return null;
	}
}
