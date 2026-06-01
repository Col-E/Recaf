package software.coley.recaf;

import jakarta.annotation.Nonnull;
import software.coley.fxaccess.AccessCheck;
import software.coley.recaf.launch.LaunchArguments;
import software.coley.recaf.launch.LaunchBootstrap;
import software.coley.recaf.launch.LaunchCommand;
import software.coley.recaf.services.plugin.PluginContainer;
import software.coley.recaf.services.plugin.PluginManager;
import software.coley.recaf.ui.config.WindowScaleConfig;
import software.coley.recaf.util.JFXValidation;
import software.coley.recaf.util.Lang;

import java.util.List;

/**
 * UI-specific launch pipeline for Recaf.
 *
 * @author Matt Coley
 */
public class UIMain {
	/**
	 * Delegate to the main entry point in the core module.
	 * <br>
	 * This largely exists so you can easily create IntelliJ run configurations without any effort.
	 * Just right-click and run this class.
	 *
	 * @param args
	 * 		Application arguments.
	 */
	public static void main(String[] args) {
		Main.main(args);
	}

	/**
	 * Launch the UI runtime using already parsed launch arguments.
	 *
	 * @param launchArgValues
	 * 		Parsed launch arguments.
	 * @param args
	 * 		Raw application arguments.
	 */
	public static void launch(@Nonnull LaunchCommand launchArgValues, @Nonnull String[] args) {
		// Add a class reference for our UI module.
		// This will get Weld to discover all services/beans in both the UI and core modules.
		Bootstrap.setWeldConsumer(weld -> weld.addPackage(true, UIMain.class));

		// Validate the JFX environment is available if not running in headless mode.
		// Abort if not available.
		int validationCode = JFXValidation.validateJFX();
		if (validationCode != 0) {
			ExitDebugLoggingHook.exit(validationCode);
			return;
		}

		// Initialize the CDI container.
		LaunchBootstrap launchBootstrap = new LaunchBootstrap(launchArgValues, args);
		Recaf recaf = launchBootstrap.bootstrap();

		// Invoke the launcher handler with headless=false since we're launching the UI.
		LaunchArguments launchArgs = launchBootstrap.getLaunchArguments();
		launchBootstrap.setupLaunchHandler(false);

		// Continue with the rest of the UI initialization and launch.
		initialize(recaf, launchArgs, launchBootstrap);
	}

	/**
	 * Initialize the UI application.
	 */
	private static void initialize(@Nonnull Recaf recaf,
	                               @Nonnull LaunchArguments launchArgs,
	                               @Nonnull LaunchBootstrap launchBootstrap) {
		launchBootstrap.initLogging();
		initFxAccessAgent();
		initTranslations();
		launchBootstrap.initPlugins();
		initPluginTranslations(recaf);
		launchBootstrap.fireInitEvent();
		initScale(recaf); // Needs to init after the init-event so config is loaded
		RecafApplication.launch(RecafApplication.class, launchArgs.getArgs());
	}

	/**
	 * Assigns UI scaling properties based on the window scale config.
	 */
	private static void initScale(@Nonnull Recaf recaf) {
		WindowScaleConfig scaleConfig = recaf.get(WindowScaleConfig.class);

		double scale = scaleConfig.getScale();
		System.setProperty("sun.java2d.uiScale", String.format("%.0f%%", 100 * scale));
		System.setProperty("glass.win.uiScale", String.valueOf(scale));
		System.setProperty("glass.gtk.uiScale", String.valueOf(scale));
	}

	/**
	 * Configure the JavaFX access logging agent.
	 * The logging is only active when the agent is passed as a launch argument to Recaf.
	 * <br>
	 * Example usage: {@code -javaagent:javafx-access-agent.jar=software/;org/;com/;javafx/}
	 */
	private static void initFxAccessAgent() {
		AccessCheck.addAccessCheckListener((className, methodName, lineNumber, threadName, calledMethodSignature) -> {
			// Some kinds of operations are safe and can be ignored.
			if (calledMethodSignature != null) {
				// Skip on constructors
				if (calledMethodSignature.contains("<"))
					return;

				// Skip on get operations
				if (calledMethodSignature.contains("#get"))
					return;

				// Skip on things that will be operated on later
				if (calledMethodSignature.contains("#setOn") || calledMethodSignature.contains("#addListener"))
					return;
			}

			System.err.printf("[thread:%s] %s.%s (line %d) - %s\n", threadName, className, methodName, lineNumber, calledMethodSignature);
		});
	}

	/**
	 * Load translations.
	 */
	private static void initTranslations() {
		Lang.initialize();
	}

	/**
	 * Load plugins.
	 */
	private static void initPluginTranslations(@Nonnull Recaf recaf) {
		PluginManager pluginManager = recaf.get(PluginManager.class);
		List<String> localeKeys = List.copyOf(Lang.getTranslationKeys());
		for (PluginContainer<?> plugin : pluginManager.getPlugins())
			Lang.loadPlugin(plugin.info().id(), plugin.plugin().getClass().getClassLoader(), localeKeys);
	}
}
