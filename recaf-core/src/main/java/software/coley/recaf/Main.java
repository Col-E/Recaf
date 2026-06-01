package software.coley.recaf;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import picocli.CommandLine;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.launch.LaunchBootstrap;
import software.coley.recaf.launch.LaunchCommand;
import software.coley.recaf.util.JdkValidation;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Application entry-point for Recaf.
 *
 * @author Matt Coley
 */
public class Main {
	private static final Logger logger = Logging.get(Main.class);

	// Class name of the UI launcher class in the UI module.
	private static final String UI_LAUNCHER_CLASS = "software.coley.recaf.UIMain";
	private static final LaunchModeHandler DEFAULT_HEADLESS_LAUNCHER = Main::launchHeadless;
	private static final LaunchModeHandler DEFAULT_GUI_LAUNCHER = Main::launchGui;
	private static final Runnable DEFAULT_JDK_VALIDATOR = JdkValidation::validateJdk;
	private static final Runnable DEFAULT_EXIT_HOOK_REGISTRAR = ExitDebugLoggingHook::register;
	static LaunchModeHandler headlessLauncher = DEFAULT_HEADLESS_LAUNCHER;
	static LaunchModeHandler guiLauncher = DEFAULT_GUI_LAUNCHER;
	static Runnable jdkValidator = DEFAULT_JDK_VALIDATOR;
	static Runnable exitHookRegistrar = DEFAULT_EXIT_HOOK_REGISTRAR;

	/**
	 * @param args
	 * 		Application arguments.
	 */
	public static void main(String[] args) {
		// Make application name appear in Mac-OS's dock.
		System.setProperty("apple.awt.application.name", "Recaf");

		// Add a shutdown hook which dumps system information to console.
		// Should provide useful information that users can copy/paste to us for diagnosing problems.
		exitHookRegistrar.run();

		// Handle explicit help before Picocli parsing since '-h' is used for headless mode.
		if (List.of(args).contains("--help")) {
			CommandLine.usage(new LaunchCommand(), System.out);
			return;
		}

		// Handle arguments.
		LaunchCommand launchArgValues = new LaunchCommand();
		try {
			CommandLine cmd = new CommandLine(launchArgValues);
			cmd.setStopAtPositional(true);
			cmd.setStopAtUnmatched(true);
			cmd.parseArgs(args);
			if (cmd.isUsageHelpRequested()) {
				cmd.usage(System.out);
				return;
			}
			if (launchArgValues.call())
				return;
		} catch (Exception ex) {
			CommandLine.usage(launchArgValues, System.out);
			return;
		}

		// Validate we're on a JDK and not a JRE.
		jdkValidator.run();

		// Launch the appropriate runtime based on the parsed launch arguments.
		if (launchArgValues.isHeadless()) {
			headlessLauncher.launch(launchArgValues, args);
		} else {
			guiLauncher.launch(launchArgValues, args);
		}
	}

	private static void launchHeadless(@Nonnull LaunchCommand launchArgValues, @Nonnull String[] args) {
		// Because we're launching in headless mode, we don't want to do a full classpath discovery/scan.
		// That would find UI service/beans which could cause problems if the user is running our smaller
		// platform independent distribution without JavaFX bundles.
		Bootstrap.enableCoreOnlyDiscovery();

		// Initialize the CDI container and launch handler.
		LaunchBootstrap launchBootstrap = new LaunchBootstrap(launchArgValues, args);
		launchBootstrap.bootstrap();
		launchBootstrap.setupLaunchHandler(true);

		// Continue with the rest of the headless initialization and launch.
		launchBootstrap.initializeHeadless();
	}

	private static void launchGui(@Nonnull LaunchCommand launchArgValues, @Nonnull String[] args) {
		try {
			// Find the UI launcher class and invoke the launch method with the parsed launch arguments.
			Class<?> uiLauncherClass = Class.forName(UI_LAUNCHER_CLASS);
			Method launch = uiLauncherClass.getDeclaredMethod("launch", LaunchCommand.class, String[].class);
			launch.invoke(null, launchArgValues, args);
		} catch (ReflectiveOperationException ex) {
			logger.error("Failed to find UI launcher '{}'", UI_LAUNCHER_CLASS, ex);
			ExitDebugLoggingHook.exit(ExitCodes.ERR_LAUNCH_NO_UI_MODULE);
		} catch (Exception ex) {
			logger.error("Failed to initialize UI launcher '{}'", UI_LAUNCHER_CLASS, ex);
			ExitDebugLoggingHook.exit(ExitCodes.ERR_LAUNCH_UI_INIT);
		}
	}

	/**
	 * Launch handler for different modes.
	 */
	@FunctionalInterface
	interface LaunchModeHandler {
		void launch(@Nonnull LaunchCommand launchArgValues, @Nonnull String[] args);
	}
}
