package dev.xdark.recaf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import dev.xdark.recaf.cli.Arguments;
import dev.xdark.recaf.util.PathUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.StandardOpenOption.*;

/**
 * Recaf's launcher entrypoint.
 *
 * @author xDark
 */
public final class Launcher {
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(Instant.class, new InstantTypeAdapter())
			.create();

	private static final String API_URL = "https://api.github.com/repos/Col-E/Recaf/releases/latest";
	public static final String ISSUES_URL = "https://github.com/Col-E/Recaf/issues";

	private static final OpenOption[] WRITE_OPTIONS = {CREATE, TRUNCATE_EXISTING};
	private static final Logger logger = LoggerFactory.getLogger("Launcher");
	private static int vmVersion = -1;
	private static Path baseDir;
	private static ReleaseInfo release;
	private static Arguments parsedArgs;

	/**
	 * Deny public constructions.
	 */
	private Launcher() {
	}

	/**
	 * Main entrypoint.
	 *
	 * @param args
	 * 		Program arguments.
	 */
	public static void main(String[] args) {
		// Print basic information about the OS and VM.
		printSysInfo();
		// Terminate if we are running on an unsupported VM.
		checkJdk();
		// Terminate if base directory cannot be located
		setBaseDir();
		// Fetch release info
		attemptFetchReleaseInfo();
		// Parse arguments
		parsedArgs = Arguments.from(args);
		// Get target jar location
		Path jarPath = parsedArgs.getJarPath();
		boolean localJarExists = Files.exists(jarPath);
		logger.info("Target jar: {}", jarPath);
		logger.info("Target jar exists: {}", localJarExists);
		// No release information? Launch with notice of release fetch failure.
		if (release == null) {
			if (localJarExists) {
				launchWithFailure(jarPath, UpdateFailure.NO_RELEASE);
			} else {
				logger.error("Launcher was unable to fetch release info, cannot continue.");
				logger.error("If you believe that it is a bug, please open an issue at:");
				logger.error(Launcher.ISSUES_URL);
			}
			return;
		}
		// Check if we want to look for a new update.
		boolean autoUpdate = parsedArgs.doAutoUpdate();
		logger.info("Automatic updates: {}", autoUpdate);
		if (!autoUpdate && localJarExists) {
			// We have opted to not check for updates.
			attemptLaunch(jarPath, Collections.emptyList());
			return;
		}
		// Check if we are even capable of updating due to file write permissions.
		boolean writeable = Files.isWritable(jarPath) && PathUtils.isWritable(jarPath);
		if (!writeable) {
			launchWithFailure(jarPath, UpdateFailure.NO_WRITE_PERMISSION);
			return;
		}
		// Fetch remote release asset information.
		Asset asset = findReleaseAsset(release.getAssets());
		if (asset == null) {
			if (localJarExists) {
				logger.error("Unable to detect release asset.");
				logger.error("Launching existing Recaf version: {}", getLocalVersion(jarPath));
				launchWithFailure(jarPath, UpdateFailure.NO_ASSET);
			} else {
				logger.error("Unable to detect release asset.\n" +
						"No local version to run. Aborting...");
			}
			return;
		}
		// Check if we should update, then run.
		boolean isUpdated = isLocalVersionUpdated(jarPath);
		boolean doUpdate = !isUpdated || isLocalVersionModified(jarPath, asset);
		if (doUpdate) {
			// Attempt to download new jar.
			if (!downloadNewJar(jarPath, asset)) {
				// Download failed, use existing jar.
				attemptLaunch(jarPath, updateFailedFlag("writeFailed"));
				return;
			}
		}
		attemptLaunch(jarPath, Collections.emptyList());
	}

	/**
	 * @param jarPath
	 * 		Destination of jar to download to.
	 * @param asset
	 * 		Asset with update information.
	 *
	 * @return {@code true} when the download is a success.
	 */
	private static boolean downloadNewJar(Path jarPath, Asset asset) {
		String url = asset.getUrl();
		logger.info("Downloading update from: {}", url);
		try {
			byte[] bytes = IOUtils.toByteArray(new URL(url));
			int actualSize = bytes.length;
			int requiredSize = asset.getSize();
			if (actualSize != requiredSize) {
				logger.warn("Size of the received jar is invalid (expected: {}, got: {})", requiredSize,
						actualSize);
			}
			logger.info("Downloaded updated jar, starting");
			Files.write(jarPath, bytes, WRITE_OPTIONS);
			return true;
		} catch (IOException ex) {
			logger.error("Unable to update jar file, is path writeable?", ex);
			return false;
		}
	}

	/**
	 * @param jarPath
	 * 		Path to local Recaf jar.
	 * @param asset
	 * 		Asset with update information.
	 *
	 * @return {@code true} if the local jar does not seem to match the remote updated jar.
	 */
	private static boolean isLocalVersionModified(Path jarPath, Asset asset) {
		if (!Files.isRegularFile(jarPath)) {
			return true;
		}
		try {
			return asset.getSize() != Files.size(jarPath);
		} catch (IOException ex) {
			logger.error("Could not verify jar size, will assume that jar is valid: ", ex);
			return false;
		}
	}

	/**
	 * @param jarPath
	 * 		Path to local Recaf jar.
	 *
	 * @return {@code true} if the local version is up to date.
	 */
	private static boolean isLocalVersionUpdated(Path jarPath) {
		// Get version from local jar
		String version = getLocalVersion(jarPath);
		if (version == null) {
			// If we can't find the version, notify user something went wrong, but it isn't totally irrecoverable
			logger.error("Version detection has failed");
			logger.error("Ensure that Recaf's jar file is not damaged, or");
			logger.error("open an issue if you think that it's an error: ");
			logger.error(ISSUES_URL);
			// Make a backup of the existing jar.
			// We will attempt to download a new one.
			try {
				Path target = jarPath.getParent().resolve(
						jarPath.getFileName().toString() + "." + System.currentTimeMillis() + ".bak");
				Files.copy(jarPath, target);
			} catch (IOException ex) {
				logger.warn("Could not copy old jar: ", ex);
			}
			return false;
		} else {
			// Check if our local version matches the latest release
			try {
				return !isOutdated(version, release.getName());
			} catch (Exception ex) {
				logger.error("Could not compare versions: ", ex);
				return false;
			}
		}
	}

	/**
	 * Launch Recaf due to a part of the update process failing.
	 *
	 * @param jarPath
	 * 		Path to Recaf jar.
	 * @param failure
	 * 		The type of failure that occurred.
	 */
	private static void launchWithFailure(Path jarPath, UpdateFailure failure) {
		logger.error(failure.getLogMessage());
		attemptLaunch(jarPath, updateFailedFlag(failure.getFlagContent()));
	}

	/**
	 * @param jar
	 * 		Path to Recaf jar.
	 * @param extraOptions
	 * 		Additional launch arguments.
	 *
	 * @return {@code true} when launch was successful. Otherwise {@code false}.
	 */
	private static boolean attemptLaunch(Path jar, List<String> extraOptions) {
		try {
			launch(jar, extraOptions);
			return true;
		} catch (IOException ex) {
			logger.error("Unable to launch Recaf: ", ex);
			return false;
		}
	}

	private static void launch(Path jar, List<String> extraOptions) throws IOException {
		Manifest manifest = getManifest(jar);
		Attributes attributes = manifest.getMainAttributes();
		Path dependenciesDir = getDependenciesDirectory();
		if (!Files.isDirectory(dependenciesDir)) {
			Files.createDirectories(dependenciesDir);
		}
		List<String> classpath = new LinkedList<>();
		classpath.add(jar.normalize().toString());

		downloadDependencies(dependenciesDir, attributes.getValue("Common-Dependencies").split(";"),
				classpath);

		try {
			Class.forName("javafx.application.Platform", false,
					Launcher.class.getClassLoader());
			logger.info("Skipping JavaFX download, seems like it is present");
		} catch (ClassNotFoundException ignored) {
			logger.error("JavaFX is not present on classpath");
			logger.error("Downloading missing JavaFX dependencies");
			// TODO: JavaFX versions are not tied to the VM version, so we should be looking for the
			//       latest stable releases available on maven central
			downloadDependencies(dependenciesDir,
					attributes.getValue(String.format("JavaFX-Dependencies-%d", getVmVersion())).split(";"),
					classpath);
		}

		List<String> command = new LinkedList<>();
		command.add(PathUtils.getJavaExecutable().toString());
		command.add("-cp");
		command.add(String.join(File.pathSeparator, classpath));
		command.add(attributes.getValue("Main-Class"));
		command.addAll(extraOptions);
		logger.info("Starting new process: {}", command);
		new ProcessBuilder()
				.directory(jar.getParent().toFile())
				.command(command)
				.start();
	}

	private static void downloadDependencies(Path dir, String[] dependencies, List<String> classpath)
			throws IOException {
		for (String url : dependencies) {
			Path path = dir.resolve(PathUtils.getCompactFileName(url));
			if (!Files.exists(path)) {
				logger.info("Downloading missing dependency: {} ----> {}", url, path);
				byte[] content = IOUtils.toByteArray(new URL(url));
				Files.write(path, content, CREATE);
			}
			classpath.add(path.toString());
		}
	}

	/**
	 * @param jarPath
	 * 		Path to Recaf jar.
	 *
	 * @return Local version of Recaf.
	 */
	private static String getLocalVersion(Path jarPath) {
		try {
			Manifest manifest = getManifest(jarPath);
			String version = manifest.getMainAttributes().getValue("Specification-Version");
			logger.info("Detected current version: {}", version);
			return version;
		} catch (IOException ex) {
			logger.error("Unable to get Recaf version: ", ex);
			return null;
		}
	}

	/**
	 * Sets Recaf's base directory.
	 */
	private static void setBaseDir() {
		try {
			baseDir = PathUtils.getRecafDirectory();
			logger.info("Recaf directory: {}", baseDir);
		} catch (IllegalStateException ex) {
			logger.error("Unable to get base directory: ", ex);
			System.exit(1);
		}
	}

	/**
	 * Log basic system information.
	 */
	private static void printSysInfo() {
		logger.info("JVM version: {}", getVmVersion());
		logger.info("JVM name: {}", System.getProperty("java.vm.name"));
		logger.info("JVM version: {}", System.getProperty("java.vm.version"));
		logger.info("JVM vendor: {}", System.getProperty("java.vm.vendor"));
		logger.info("Java home: {}", PathUtils.getJavaHome());
		logger.info("Java executable: {}", PathUtils.getJavaExecutable());
		logger.info("OS name: {}", System.getProperty("os.name"));
		logger.info("OS Arch: {}", System.getProperty("os.arch"));
	}

	/**
	 * Checks JDK version.
	 */
	private static void checkJdk() {
		if (getVmVersion() < 11) {
			logger.error("Recaf requires JDK 11+ in order to run");
			System.exit(1);
		}
	}

	private static void attemptFetchReleaseInfo() {
		try {
			release = GSON.fromJson(IOUtils.toString(new URL(API_URL),
					StandardCharsets.UTF_8), ReleaseInfo.class);
			logger.info("Successfully fetched release info, found: {}", release.getName());
		} catch (IOException ex) {
			logger.error("Could not fetch release info: ", ex);
		}
	}

	private static boolean isOutdated(String current, String latest) {
		String[] arrSelf = current.split("\\.");
		String[] arrLatest = latest.split("\\.");
		if (arrSelf.length == arrLatest.length) {
			for (int i = 0; i < arrSelf.length; i++) {
				int iSelf = Integer.parseInt(arrSelf[i]);
				int iLatest = Integer.parseInt(arrLatest[i]);
				if (iSelf != iLatest) {
					return iSelf < iLatest;
				}
			}
		}
		return false;
	}

	private static Asset findReleaseAsset(List<Asset> assets) {
		return assets.stream().filter(asset -> asset.getName().matches("recaf-*.*.*.jar")).findFirst()
				.orElse(null);
	}

	private static Path getDependenciesDirectory() {
		return PathUtils.getRecafDirectory().resolve("dependencies");
	}

	private static Manifest getManifest(Path path) throws IOException {
		try (JarFile jar = new JarFile(path.toFile())) {
			Manifest manifest = jar.getManifest();
			return new Manifest(manifest);
		}
	}

	private static int getVmVersion() {
		int vmVersion = Launcher.vmVersion;
		if (vmVersion == -1) {
			String property = System.getProperty("java.class.version", "");
			if (!property.isEmpty()) {
				return Launcher.vmVersion = (int) (Float.parseFloat(property) - 44);
			}
			logger.warn("Using fallback vm-version fetch, no value for 'java.class.version'");
			property = System.getProperty("java.vm.specification.version", "");
			if (property.contains(".")) {
				return Launcher.vmVersion = (int) Float
						.parseFloat(property.substring(property.indexOf('.') + 1));
			} else if (!property.isEmpty()) {
				return Launcher.vmVersion = Integer.parseInt(property);
			}
			logger.warn("Fallback vm-version fetch failed, defaulting to 8");
			return Launcher.vmVersion = 8;
		}
		return vmVersion;
	}

	private static List<String> updateFailedFlag(String reason) {
		return Collections.singletonList(String.format("--updateFailed=%s", reason));
	}
}
