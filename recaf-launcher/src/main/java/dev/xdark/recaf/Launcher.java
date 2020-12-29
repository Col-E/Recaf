package dev.xdark.recaf;

import com.google.gson.GsonBuilder;
import dev.dirs.BaseDirectories;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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

  private static final String API_URL = "https://api.github.com/repos/Col-E/Recaf/releases/latest";
  private static final String ISSUES_URL = "https://github.com/Col-E/Recaf/issues";

  private static final OpenOption[] WRITE_OPTIONS = {StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING};
  private static final List<String> UPDATE_FAIL = Collections.singletonList("--updateFailed");

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
        .defaultsTo(getDirectory().resolve("Recaf.jar"));
    OptionSpec<Boolean> autoUpdateOption = parser
        .accepts("autoUpdate", "Should the update be done automatically?")
        .withRequiredArg()
        .ofType(Boolean.class)
        .defaultsTo(true);
    parser.allowsUnrecognizedOptions();
    OptionSet options = parser.parse(args);
    Path jarPath = options.valueOf(jarOption);
    logger.info("Target jar: {}", jarPath);
    boolean exists = Files.exists(jarPath);

    logger.info("Target jar exists: {}", exists);
    if (info == null) {
      // We were unable to fetch latest release info.
      // Attempt to continue.
      if (exists) {
        attemptLaunch(jarPath, UPDATE_FAIL);
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
      try (JarFile jar = new JarFile(jarPath.toFile())) {
        Manifest manifest = jar.getManifest();
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
        attemptLaunch(jarPath, UPDATE_FAIL, 1);
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
          attemptLaunch(jarPath, UPDATE_FAIL, 1);
        }
      }
      // Download new jar.
      logger.info("Downloading update from: {}", asset.getUrl());
      byte[] bytes;
      try {
        URL url = new URL(asset.getUrl());
        try (InputStream in = url.openStream()) {
          bytes = IOUtils.toByteArray(in);
        }
        int actualSize = bytes.length;
        int requiredSize = asset.getSize();
        if (actualSize != requiredSize) {
          logger.error("Size of the received jar is invalid (expected: {}, got: {})", requiredSize, actualSize);
        }
        logger.info("Downloaded updated jar, starting");
        Files.write(jarPath, bytes, WRITE_OPTIONS);
        launch(jarPath, Collections.emptyList());
      } catch (IOException ex) {
        logger.error("Unable to update jar file, is path writeable?", ex);
        if (exists) {
          attemptLaunch(jarPath, UPDATE_FAIL, 1);
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
    List<String> command = new LinkedList<>(
        Arrays.asList("java", "-jar", jar.normalize().toString()));
    command.addAll(extraOptions);
    new ProcessBuilder()
        .directory(jar.getParent().toFile())
        .command(command)
        .start();
  }

  private static ReleaseInfo fetchReleaseInfo() throws IOException {
    URL url = new URL(API_URL);
    String content;
    try (InputStream in = url.openStream()) {
      content = IOUtils.toString(in, StandardCharsets.UTF_8);
    }
    ReleaseInfo info = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
        .create()
        .fromJson(content, ReleaseInfo.class);
    return info;
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

  private static Path getDirectory() {
    Path directory;
    try {
      directory = Paths.get(BaseDirectories.get().configDir).resolve("Recaf");
    } catch (Throwable t) {
      if (System.getProperty("os.name").toLowerCase().contains("win")) {
        directory = Paths.get(System.getenv("APPDATA"), "Recaf");
      } else {
        throw new IllegalStateException("Failed to initialize Recaf directory");
      }
    }
    return directory;
  }
}
