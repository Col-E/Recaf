package dev.xdark.recaf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.dirs.BaseDirectories;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recaf's launcher entrypoint.
 *
 * @author xDark
 */
public final class Launcher {

  private static final Logger logger = LoggerFactory.getLogger("Launcher");
  private static final Gson GSON = new GsonBuilder()
      .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
      .create();

  private static final String API_URL = "https://api.github.com/repos/Col-E/Recaf/releases/latest";
  private static final String ISSUES_URL = "https://github.com/Col-E/Recaf/issues";

  private static final OpenOption[] WRITE_OPTIONS = {StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING};
  private static Path recafDirectory;
  private static int vmVersion = -1;

  /**
   * Deny public constructions.
   */
  private Launcher() {
  }

  /**
   * Main entrypoint.
   *
   * @param args Program arguments.
   */
  public static void main(String[] args) {
    // Print basic information about the OS and VM.
    logger.info("JVM version: {}", getVmVersion());
    logger.info("JVM name: {}", System.getProperty("java.vm.name"));
    logger.info("JVM version: {}", System.getProperty("java.vm.version"));
    logger.info("JVM vendor: {}", System.getProperty("java.vm.vendor"));
    logger.info("Java home: {}", getJavaHome());
    logger.info("Java executable: {}", getJavaExecutable());
    logger.info("OS name: {}", System.getProperty("os.name"));
    logger.info("OS Arch: {}", System.getProperty("os.arch"));

    Path baseDir;
    try {
      baseDir = getDirectory();
    } catch (IllegalStateException ex) {
      logger.error("Unable to get base directory: ", ex);
      System.exit(1);
      return;
    }

    logger.info("Recaf directory: {}", baseDir);
    ReleaseInfo info = null;
    try {
      info = fetchReleaseInfo();
      logger.info("Successfully fetched release info");
    } catch (IOException ex) {
      logger.error("Could not fetch release info: ", ex);
    }

    OptionParser parser = new OptionParser();
    OptionSpec<Path> jarOption = parser.accepts("home", "Recaf jar file")
        .withRequiredArg()
        .withValuesConvertedBy(new PathValueConverter())
        .defaultsTo(baseDir.resolve("Recaf.jar"));
    OptionSpec<Boolean> autoUpdateOption = parser
        .accepts("autoUpdate", "Should the update be done automatically?")
        .withRequiredArg()
        .ofType(Boolean.class)
        .defaultsTo(true);
    parser.allowsUnrecognizedOptions();
    OptionSet options = parser.parse(args);
    List<?> unknown = options.nonOptionArguments();
    if (!unknown.isEmpty()) {
      logger.warn("Ignoring unknown options: {}", unknown);
    }
    Path jarPath = options.valueOf(jarOption);
    logger.info("Target jar: {}", jarPath);
    boolean exists = Files.exists(jarPath);

    logger.info("Target jar exists: {}", exists);
    if (info == null) {
      // We were unable to fetch latest release info.
      // Attempt to continue.
      if (exists) {
        attemptLaunch(jarPath, updateFailedFlag("missingInfo"));
      } else {
        logger.error("Launcher was unable to fetch release info, cannot continue");
        logger.error("If you believe that it is a bug, please open an issue at: ");
        logger.error(ISSUES_URL);
        System.exit(1);
      }
    }
    boolean autoUpdate = options.valueOf(autoUpdateOption);
    logger.info("Automatic updates: {}", autoUpdate);
    boolean versionValid = false;
    if (exists) {
      String version = null;
      try {
        Manifest manifest = getManifest(jarPath);
        version = manifest.getMainAttributes().getValue("Specification-Version");
        logger.info("Detected current version: {}", version);
      } catch (IOException ex) {
        logger.error("Unable to get Recaf version: ", ex);
      }
      if (version == null) {
        logger.error("Version detected has failed");
        logger.error("Ensure that Recaf's jar file is not damaged, or");
        logger.error("open an issue if you think that it's an error: ");
        logger.error(ISSUES_URL);
        try {
          Path target = jarPath.getParent().resolve(
              jarPath.getFileName().toString() + "." + System.currentTimeMillis() + ".bak");
          Files.copy(jarPath, target);
        } catch (IOException ex) {
          logger.warn("Could not copy old jar: ", ex);
        }
        versionValid = false;
      } else {
        try {
          versionValid = !isOutdated(version, info.getName());
        } catch (Exception ex) {
          logger.error("Could not compare versions: ", ex);
          versionValid = false;
        }
      }
    }
    Asset asset = findReleaseAsset(info.getAssets());
    if (asset == null) {
      logger.error("Unable to detect release asset");
    }
    boolean areSizesEqual = false;
    if (versionValid) {
      if (asset == null) {
        logger.error("Launcher was unable to detect release asset from GitHub releases");
        logger.error("Please open an issue at: ");
        logger.error(ISSUES_URL);
        attemptLaunch(jarPath, updateFailedFlag("missingAsset"), 1);
      }
      try {
        areSizesEqual = asset.getSize() == Files.size(jarPath);
        logger.info("Jar size match: {}", areSizesEqual);
      } catch (IOException ex) {
        logger.error("Could not verify jar size, will assume that jar is valid: ", ex);
      }
    }
    if (!areSizesEqual) {
      if (exists) {
        boolean writeable = Files.isWritable(jarPath);
        if (!writeable) {
          logger.error("Jar is not writeable, check your file system permissions");
          attemptLaunch(jarPath, updateFailedFlag("notWriteable"), 1);
        }
      }
      // Download new jar.
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
        launch(jarPath, Collections.emptyList());
      } catch (IOException ex) {
        logger.error("Unable to update jar file, is path writeable?", ex);
        if (exists) {
          attemptLaunch(jarPath, updateFailedFlag("writeFailed"), 1);
        }
      }
    } else {
      logger.info("Current jar seems to be valid, using it");
      attemptLaunch(jarPath, Collections.emptyList());
    }
  }

  private static void attemptLaunch(Path jar, List<String> extraOptions, int exit) {
    try {
      launch(jar, extraOptions);
      System.exit(exit);
    } catch (IOException ex) {
      logger.error("Unable to launch Recaf: ", ex);
      System.exit(1);
    }
  }

  private static void attemptLaunch(Path jar, List<String> extraOptions) {
    attemptLaunch(jar, extraOptions, 0);
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

    JsonArray dependencies = GSON
        .fromJson(attributes.getValue("Common-Dependencies"), JsonArray.class);
    downloadDependencies(dependenciesDir, dependencies, classpath);

    try {
      Class.forName("javafx.application.Platform", false,
          Launcher.class.getClassLoader());
      logger.info("Skipping JavaFX download, seems like it is present");
    } catch (ClassNotFoundException ignored) {
      logger.error("JavaFX is not present on classpath");
      int version = getVmVersion();
      if (version >= 11) {
        logger.error("Downloading missing JavaFX dependencies");
        JsonArray jfx = GSON.fromJson(attributes.getValue(String.format("JavaFX-Dependencies-%d",
            version)), JsonArray.class);
        downloadDependencies(dependenciesDir, jfx, classpath);
      } else {
        logger.error("Incompatible JVM version: {}", version);
        logger.error("Please upgrade your installation to JDK 11+");
        logger.error("Or install JDK that bundles JavaFX");
        System.exit(1);
      }
    }

    List<String> command = new LinkedList<>();
    command.add(getJavaExecutable().toString());
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

  private static void downloadDependencies(Path dir, JsonArray dependencies, List<String> classpath)
      throws IOException {
    for (JsonElement dependency : dependencies) {
      String url = dependency.getAsString();
      Path path = dir.resolve(getCompactFileName(url));
      if (!Files.exists(path)) {
        logger.info("Downloading missing dependency: {} ----> {}", url, path);
        byte[] content = IOUtils.toByteArray(new URL(url));
        Files.write(path, content, StandardOpenOption.CREATE);
      }
      classpath.add(path.toString());
    }
  }

  private static ReleaseInfo fetchReleaseInfo() throws IOException {
    return GSON.fromJson(IOUtils.toString(new URL(API_URL),
        StandardCharsets.UTF_8), ReleaseInfo.class);
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

  private static Path getJavaHome() {
    return Paths.get(System.getProperty("java.home"));
  }

  private static Path getJavaExecutable() {
    Path javaHome = getJavaHome();
    Path bin = javaHome.resolve("bin");
    if (Platform.isOnWindows()) {
      return bin.resolve("java.exe");
    }
    return bin.resolve("java");
  }

  private static Path getDirectory() {
    Path directory = recafDirectory;
    if (directory == null) {
      try {
        directory = Paths.get(BaseDirectories.get().configDir).resolve("Recaf");
      } catch (Throwable t) {
        if (!Platform.isOnWindows()) {
          throw new IllegalStateException("Failed to initialize Recaf directory", t);
        }
        logger.error("Could not get base directory: ", t);
        logger.error("Using %APPDATA%/Recaf as base directory");
      }
      if (directory == null) {
        directory = Paths.get(System.getenv("APPDATA"), "Recaf");
      }
      recafDirectory = directory;
    }
    return directory;
  }

  private static Path getDependenciesDirectory() {
    return getDirectory().resolve("dependencies");
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

  private static int indexOfLastSeparator(String fileName) {
    int lastUnixPos = fileName.lastIndexOf('/');
    int lastWindowsPos = fileName.lastIndexOf('\\');
    return Math.max(lastUnixPos, lastWindowsPos);
  }

  private static String getCompactFileName(String fileName) {
    int index = indexOfLastSeparator(fileName);
    return fileName.substring(index + 1);
  }

  private static List<String> updateFailedFlag(String reason) {
    return Collections.singletonList(String.format("--updateFailed=%s", reason));
  }
}
