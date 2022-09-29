package me.coley.recaf.presentation;

import dev.xdark.recaf.plugin.RecafPluginManager;
import javafx.application.Platform;
import me.coley.recaf.Controller;
import me.coley.recaf.RecafConstants;
import me.coley.recaf.RecafUI;
import me.coley.recaf.config.ConfigRegistry;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.prompt.WorkspaceIOPrompts;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.*;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.ThreadUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

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
		RecafUI.set(controller);
		// Patch JDK restrictions
		AccessPatcher.patch();
		// Setup JavaFX
		JFXInjection.ensureJavafxSupport();
		// Load translations
		Lang.initialize();
		// Setup config
		Configs.containers().forEach(ConfigRegistry::register);
		try {
			ConfigRegistry.load();
			// Use loaded values
			Configs.recentWorkspaces().update();
			WorkspaceIOPrompts.setupLocations();
		} catch (IOException ex) {
			logger.error("Failed to load stored config values", ex);
		}
		// Load language from config
		Lang.setCurrentTranslations(Configs.display().lang);
		// Setup listener to ensure we update classpath dependency directories
		CompileDependencyUpdater.install(controller);
		DecompileBytecodePatcher.install(controller);
		DecompileTextPatcher.install(controller);
		// Enable plugins that are marked as active
		RecafPluginManager.getInstance().enablePlugins(Configs.plugin().enabledState
				.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet())
		);
		// Call 'getSystemClasses' in a background thread so that later usage can use the cached value
		ThreadUtil.run(ClasspathUtil::getSystemClasses);
		// Open UI
		JFXUtils.initializePlatform(() -> {
			try {
				RecafUI.initialize();
				RecafUI.getWindows().getMainWindow().show();
			} catch (Throwable ex) {
				logger.error("Recaf crashed due to an unhandled error." +
						"Please open a bug report: " + RecafConstants.getUrlBugReport(), ex);
				Platform.exit();
				System.exit(-1);
			}
		});
		// Intercept / log uncaught exceptions
		Thread.UncaughtExceptionHandler exceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
			// TODO: When updating to newer RichTextFX, remove this (its been fixed)
			if (exception.getMessage() != null &&
					exception.getMessage().contains("Visible paragraphs' last index is [-1]"))
				return;

			logger.error("Uncaught exception on thread '" + thread.getName() + "'", exception);
			if (exceptionHandler != null)
				exceptionHandler.uncaughtException(thread, exception);
		});
		// Shutdown handler
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				ConfigRegistry.save();
			} catch (IOException ex) {
				logger.error("Failed to save config values", ex);
			}
			ThreadUtil.shutdown();
		}));
	}

	@Override
	public WorkspacePresentation workspaceLayer() {
		return new GuiWorkspacePresentation(controller);
	}
}
