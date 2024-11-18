package software.coley.recaf;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.file.RecafDirectoriesConfig;
import software.coley.recaf.util.JavaVersion;
import software.coley.recaf.util.LookupUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.ModuleReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A hook registered to {@code java.lang.Shutdown} and not {@link Runtime#addShutdownHook(Thread)}.
 * This allows us to use the {@link StackWalker} API to detect the exit code from {@link System#exit(int)}
 *
 * @author Matt Coley
 */
public class ExitDebugLoggingHook {
	private static final int UNKNOWN_CODE = -1337420;
	private static final Logger logger = Logging.get(ExitDebugLoggingHook.class);
	private static int exitCode = UNKNOWN_CODE;
	private static boolean printConfigs;
	private static Thread mainThread;
	private static MethodHandles.Lookup lookup;

	/**
	 * Register the shutdown hook.
	 */
	public static void register() {
		lookup = LookupUtil.lookup();
		try {
			// We use this instead of the Runtime shutdown hook thread because this will run on the same thread
			// as the call to System.exit(int)
			Class<?> shutdown = lookup.findClass("java.lang.Shutdown");
			MethodHandle add = lookup.findStatic(shutdown, "add", MethodType.methodType(void.class, int.class, boolean.class, Runnable.class));
			add.invoke(9, false, (Runnable) ExitDebugLoggingHook::run);
		} catch (Throwable t) {
			logger.error("Failed to add exit-hooking debug dumping shutdown hook", t);

			// Use fallback shutdown hook which checks for manual exit codes being set.
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				if (exitCode != UNKNOWN_CODE)
					handle(exitCode);
			}));
		}
	}

	private static void run() {
		// If we've set the exit code manually, use that.
		if (exitCode != UNKNOWN_CODE) {
			handle(exitCode);
			return;
		}

		// We didn't do the exit, so lets try and see if we can figure out who did it.
		try {
			AtomicBoolean visited = new AtomicBoolean();
			Class<?> shutdown = lookup.findClass("java.lang.Shutdown");
			Class<?> stackFrameClass = Class.forName("java.lang.LiveStackFrame");
			MethodHandle getLocals = lookup.findVirtual(stackFrameClass, "getLocals", MethodType.methodType(Object[].class))
					.asType(MethodType.methodType(Object[].class, Object.class));
			Method getStackWalker = stackFrameClass.getDeclaredMethod("getStackWalker", Set.class);
			getStackWalker.setAccessible(true);
			StackWalker stackWalker = (StackWalker) getStackWalker.invoke(null, Set.of(StackWalker.Option.RETAIN_CLASS_REFERENCE, StackWalker.Option.SHOW_HIDDEN_FRAMES));
			stackWalker.forEach(frame -> {
				try {
					if (!visited.get() && frame.getDeclaringClass() == shutdown && frame.getMethodName().equals("exit")) {
						Object[] locals = (Object[]) getLocals.invoke(frame);
						String local0 = locals[0].toString().replaceAll("\\D+", "");
						int exit = (int) Long.parseLong(local0); // Need to parse as long, cast to int
						handle(exit);
						visited.set(true);
					}
				} catch (Throwable t) {
					throw new IllegalStateException(t);
				}
			});
		} catch (Throwable t) {
			// If this cursed abomination breaks, we want to know about it
			logger.error("""
					Failed to detect application exit code.

					Please report that the exit debugger has failed.

					  https://github.com/Col-E/Recaf/issues/new?labels=bug&title=Error%20Debugger%20Hook%20fails%20on%20Java%20{V}
					""".replace("{V}", String.valueOf(JavaVersion.get())), t);
		}
	}

	private static void handle(int code) {
		// Skip on successful closure
		if (code == ExitCodes.SUCCESS || code == ExitCodes.INTELLIJ_TERMINATION)
			return;

		if (code == UNKNOWN_CODE)
			System.out.println("Exit code: <error>");
		else
			System.out.println("Exit code: " + code);

		System.out.println("Java");
		System.out.println(" - Version (Runtime): " + System.getProperty("java.runtime.version", "<unknown>"));
		System.out.println(" - Version (Raw):     " + JavaVersion.get());
		System.out.println(" - Vendor:            " + System.getProperty("java.vm.vendor", "<unknown>"));
		System.out.println(" - Home:              " + System.getProperty("java.home", "<unknown>"));

		System.out.println("JavaFX");
		System.out.println(" - Version (Runtime): " + System.getProperty("javafx.runtime.version", "<uninitialized>"));
		System.out.println(" - Version (Raw):     " + System.getProperty("javafx.version", "<uninitialized>"));
		{
			ClassLoader loader = ExitDebugLoggingHook.class.getClassLoader();
			String javafxClass = "javafx/beans/Observable.class";
			try {
				Iterator<URL> iterator = loader.getResources(javafxClass).asIterator();
				if (!iterator.hasNext()) {
					System.out.println(" - Location: not found");
				} else {
					URL url = iterator.next();
					if (!iterator.hasNext()) {
						System.out.println(" - Location:          " + url);
					} else {
						System.out.println(" - Location (likely): " + url);
						do {
							System.out.println(" - Location (seen):   " + url);
						} while (iterator.hasNext());
					}
				}
			} catch (Exception ex) {
				System.out.println(" - Location:   <error>");
			}
		}

		System.out.println("Operating System");
		System.out.println(" - Name:           " + System.getProperty("os.name"));
		System.out.println(" - Version:        " + System.getProperty("os.version"));
		System.out.println(" - Architecture:   " + System.getProperty("os.arch"));
		System.out.println(" - Processors:     " + Runtime.getRuntime().availableProcessors());
		System.out.println(" - Path Separator: " + File.pathSeparator);
		System.out.println("Recaf");
		System.out.println(" - Version:    " + RecafBuildConfig.VERSION);
		System.out.println(" - Build hash: " + RecafBuildConfig.GIT_SHA);
		System.out.println(" - Build date: " + RecafBuildConfig.GIT_DATE);

		String command = System.getProperty("sun.java.command", "");
		if (command != null) {
			System.out.println("Launch");
			System.out.println(" - Args: " + command);
		}

		String[] classPath = System.getProperty("java.class.path").split(File.pathSeparator);
		System.out.println("Classpath:");
		for (String pathEntry : classPath) {
			File file = new File(pathEntry);
			if (file.isFile()) {
				System.out.println(" - File: " + pathEntry);
				try {
					System.out.println("   - SHA1: " + createSha1(file));
				} catch (Exception ex) {
					System.out.println("   - SHA1: <error>");
				}
			} else if (file.isDirectory()) {
				System.out.println(" - Directory: " + pathEntry);
			}
		}

		System.out.println("Boot class loader:");
		dumpBootstrapClassLoader();
		System.out.println("Platform class loader:");
		dumpBuiltinClassLoader(ClassLoader.getPlatformClassLoader());

		Path root = RecafDirectoriesConfig.createBaseDirectory().resolve("config");
		if (printConfigs && Files.isDirectory(root)) {
			System.out.println("Configs");
			try (Stream<Path> stream = Files.walk(root)) {
				stream.filter(path -> path.getFileName().toString().endsWith("-config.json")).forEach(path -> {
					String configName = path.getFileName().toString();

					// Skip certain configs
					if (configName.contains("service.ui.bind-"))
						return;
					if (configName.contains("service.ui.snippets-config"))
						return;
					if (configName.contains("service.io.recent-workspaces-config"))
						return;
					if (configName.contains("service.decompile.impl.decompiler-"))
						return;

					System.out.println(" - " + configName + ":");
					try {
						String indent = "    ";
						String configJson = Files.readString(path);
						String indented = indent + Arrays.stream(configJson.split("\n"))
								.collect(Collectors.joining("\n" + indent));
						System.out.println(indented);
					} catch (Throwable t) {
						System.out.println(" - <error>");
					}
				});
			} catch (Exception ex) {
				System.out.println(" - <error>");
			}
		}

		System.out.println("Threads");
		Thread.getAllStackTraces().forEach((thread, trace) -> {
			System.out.println(" - " + thread.getName() + " [" + thread.getState().name() + "]");
			for (StackTraceElement element : trace) {
				System.out.println("   - " + element);
			}
		});
	}

	private static void dumpBuiltinClassLoader(ClassLoader loader) {
		try {
			Class<?> c = Class.forName("jdk.internal.loader.BuiltinClassLoader");
			Field nameToModuleField = c.getDeclaredField("nameToModule");
			nameToModuleField.setAccessible(true);
			//noinspection unchecked
			Map<String, ModuleReference> mdouleMap = (Map<String, ModuleReference>) nameToModuleField.get(loader);
			for (Map.Entry<String, ModuleReference> e : mdouleMap.entrySet()) {
				ModuleReference moduleReference = e.getValue();
				System.out.printf("%s located at %s%n", moduleReference.descriptor().toNameAndVersion(), moduleReference.location()
						.map(URI::toString)
						.orElse("Unknown"));
			}
		} catch (Exception ex) {
			System.out.printf("dumpBuiltinClassLoader(%s) - <error>%n", loader);
		}
	}

	private static void dumpBootstrapClassLoader() {
		try {
			Class<?> c = Class.forName("jdk.internal.loader.ClassLoaders");
			Method bootLoaderMethod = c.getDeclaredMethod("bootLoader");
			bootLoaderMethod.setAccessible(true);
			dumpBuiltinClassLoader((ClassLoader) bootLoaderMethod.invoke(null));
		} catch (Exception ex) {
			System.out.println("dumpBootstrapClassLoader - <error>");
		}
	}

	@Nonnull
	@SuppressWarnings("all")
	private static String createSha1(@Nonnull File file) throws Exception {
		long length = file.length();
		if (length > Integer.MAX_VALUE)
			throw new IOException("File too large to hash");
		Hasher hasher = Hashing.sha1().newHasher((int) length);
		try (InputStream fis = new FileInputStream(file)) {
			int n = 0;
			byte[] buffer = new byte[8192];
			while (n != -1) {
				n = fis.read(buffer);
				if (n > 0)
					hasher.putBytes(buffer, 0, n);
			}
			return hasher.hash().toString();
		}
	}

	public static void exit(int exitCode) {
		ExitDebugLoggingHook.exitCode = exitCode;
		System.exit(exitCode);
	}
}
