package software.coley.recaf;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.inject.se.SeContainer;
import org.jboss.weld.environment.se.Weld;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.cdi.EagerInitializationExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static software.coley.recaf.RecafBuildConfig.*;

/**
 * Handles creation of Recaf instance.
 *
 * @author Matt Coley
 */
public class Bootstrap {
	private static final Logger logger = Logging.get(Bootstrap.class);
	private static final String RESOURCE_PATH = "META-INF/recaf-core-beans.txt";
	private static Recaf instance;
	private static Consumer<Weld> weldConsumer;
	private static boolean coreOnlyDiscovery;

	/**
	 * @return Recaf instance.
	 */
	@Nonnull
	public static Recaf get() {
		if (instance == null) {
			String fmt = """
					Initializing Recaf {}
					 - Build rev:  {}
					 - Build date: {}
					 - Build hash: {}""";
			logger.info(fmt, VERSION, GIT_REVISION, GIT_DATE, GIT_SHA);
			long then = System.currentTimeMillis();

			// Create the Recaf container
			try {
				SeContainer container = createContainer();
				instance = new Recaf(container);
				logger.info("Recaf CDI container created in {}ms", System.currentTimeMillis() - then);
			} catch (Throwable t) {
				logger.error("Failed to create Recaf CDI container", t);
				ExitDebugLoggingHook.exit(ExitCodes.ERR_CDI_INIT_FAILURE);
			}
		}
		return instance;
	}

	/**
	 * Must be called before invoking {@link #get()}.
	 *
	 * @param consumer
	 * 		Consumer to operate on the CDI container producing {@link Weld} instance.
	 */
	public static void setWeldConsumer(@Nullable Consumer<Weld> consumer) {
		weldConsumer = consumer;
	}

	/**
	 * Restrict bean discovery to the core module's CDI beans.
	 * Must be called before invoking {@link #get()}.
	 */
	public static void enableCoreOnlyDiscovery() {
		coreOnlyDiscovery = true;
	}

	@Nonnull
	private static SeContainer createContainer() {
		logger.info("Creating Recaf CDI container...");
		Weld weld = new Weld("recaf");
		weld.setClassLoader(Bootstrap.class.getClassLoader());

		// Setup custom interceptors & extensions
		logger.info("CDI: Adding interceptors & extensions");
		weld.addExtension(EagerInitializationExtension.getInstance());

		// Setup bean discovery
		if (coreOnlyDiscovery) {
			logger.info("CDI: Registering core bean archive");
			configureCoreBeans(weld);
		} else {
			logger.info("CDI: Registering bean packages");
			weld.addPackage(true, Recaf.class);
		}

		// Handle user-defined action
		if (weldConsumer != null) {
			logger.info("CDI: Running user-defined Consumer<Weld>");
			weldConsumer.accept(weld);
			weldConsumer = null;
		}

		logger.info("CDI: Initializing...");
		try {
			return weld.initialize();
		} finally {
			coreOnlyDiscovery = false;
		}
	}

	/**
	 * Disable classpath discovery and register the core bean archive explicitly.
	 *
	 * @param weld
	 * 		Weld container builder.
	 */
	public static void configureCoreBeans(@Nonnull Weld weld) {
		ClassLoader classLoader = Bootstrap.class.getClassLoader();
		weld.disableDiscovery();
		for (String beanClassName : loadBeanClassNames(classLoader))
			weld.addBeanClass(loadBeanClass(classLoader, beanClassName));
	}

	@Nonnull
	private static List<String> loadBeanClassNames(@Nonnull ClassLoader classLoader) {
		try (InputStream stream = classLoader.getResourceAsStream(RESOURCE_PATH)) {
			if (stream == null)
				throw new IllegalStateException("Missing core bean index: " + RESOURCE_PATH);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				return reader.lines()
						.filter(line -> !line.isBlank())
						.collect(Collectors.toList());
			}
		} catch (IOException ex) {
			throw new IllegalStateException("Failed reading core bean index: " + RESOURCE_PATH, ex);
		}
	}

	@Nonnull
	private static Class<?> loadBeanClass(@Nonnull ClassLoader classLoader, @Nonnull String className) {
		try {
			return Class.forName(className, false, classLoader);
		} catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Failed to load core bean class: " + className, ex);
		}
	}
}
