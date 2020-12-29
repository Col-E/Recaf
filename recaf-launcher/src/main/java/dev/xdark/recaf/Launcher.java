package dev.xdark.recaf;

import com.google.gson.GsonBuilder;
import dev.dirs.BaseDirectories;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

/**
 * Recaf's launcher entrypoint.
 *
 * @author xDark
 */
public final class Launcher {

  private static final String API_URL = "https://api.github.com/repos/Col-E/Recaf/releases/latest";
  private static final String ISSUES_URL = "https://github.com/Col-E/Recaf/issues";

  private static final List<String> UPDATE_FAIL = Collections.singletonList("--updateFailed");
  private static final PrintStream PS = System.err;

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
      log("Successfully fetched release info");
    } catch (IOException ex) {
      log("Could not fetch release info: ", ex);
    }

    OptionParser parser = new OptionParser();
    OptionSpec<Path> jarOption = parser.accepts("home", "Recaf jar file")
        .withRequiredArg()
        .withValuesConvertedBy(new PathValueConverter())
        .defaultsTo(Paths.get(BaseDirectories.get().dataDir).resolve("Recaf").resolve("Recaf.jar"));
    OptionSpec<Boolean> autoUpdateOption = parser
        .accepts("autoUpdate", "Should the update be done automatically?")
        .withRequiredArg()
        .ofType(Boolean.class)
        .defaultsTo(true);
    parser.allowsUnrecognizedOptions();
    OptionSet options = parser.parse(args);
    Path jarPath = options.valueOf(jarOption);
    boolean exists = Files.exists(jarPath), valid = exists;
    log("Target jar exists: %s", exists);
    if (info == null) {
      // We were unable to fetch latest release info.
      // Attempt to continue.
      if (valid) {
        attemptLaunch(jarPath, UPDATE_FAIL);
      } else {
        log("Launcher was unable to fetch release info, cannot continue.");
        log("If you believe that it is a bug, please open an issue at: ");
        log(ISSUES_URL);
        System.exit(1);
      }
    }
    boolean autoUpdate = options.valueOf(autoUpdateOption);
    log("Automatic updates: %s", autoUpdate);
    if (valid) {
      String version = null;
      try (JarFile jar = new JarFile(jarPath.toFile())) {
        Manifest manifest = jar.getManifest();
        version = manifest.getMainAttributes().getValue("Specification-Version");
        log("Detected current version: %s", version);
      } catch (IOException ex) {
        log("Unable to get Recaf version: ", ex);
      }
      if (version == null) {
        log("Version detected has failed");
        log("Ensure that Recaf's jar file is not damaged, or");
        log("Open an issue if you think that it's an error: ");
        log(ISSUES_URL);
        valid = false;
      } else {
        try {
          valid = !isOutdated(version, info.getName());
        } catch (Exception ex) {
          log("Could not compare versions: ", ex);
          valid = false;
        }
      }
    }
    Asset asset = findReleaseAsset(info.getAssets());
    if (asset == null) {
      log("Unable to detect release asset");
    }
    if (valid) {
      if (asset == null) {
        log("Launcher was unable to detect release asset from GitHub releases");
        log("Please open an issue at: ");
        log(ISSUES_URL);
        attemptLaunch(jarPath, UPDATE_FAIL, 1);
      }
      try {
        valid = asset.getSize() == Files.size(jarPath);
        log("Jar size match: %s", valid);
      } catch (IOException ex) {
        log("Could not verify jar size, will assume that jar is valid: ", ex);
      }
    }
    if (!valid) {
      if (exists) {
        boolean writeable = Files.isWritable(jarPath);
        if (!writeable) {
          log("Jar is not writeable, check your file system permissions");
          attemptLaunch(jarPath, UPDATE_FAIL, 1);
        }
      }
      // Download new jar.
      String $url = asset.getUrl();
      log("Downloading update from: %s", $url);
      byte[] bytes;
      try {
        URL url = new URL($url);
        try (InputStream in = url.openStream()) {
          bytes = IOUtils.toByteArray(in);
        }
        log("Downloaded updated jar, starting");
        Files
            .write(jarPath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        launch(jarPath, Collections.emptyList());
      } catch (IOException ex) {
        log("Unable to update jar file, is path writeable? ", ex);
        if (exists) {
          attemptLaunch(jarPath, UPDATE_FAIL, 1);
        }
      }
    } else {
      log("Current jar seems to be valid, using it");
      attemptLaunch(jarPath, Collections.emptyList());
    }
  }

  private static void attemptLaunch(Path jar, List<String> extraOptions, int exit) {
    try {
      launch(jar, extraOptions);
      System.exit(exit);
    } catch (IOException ex) {
      log("Unable to launch Recaf: ", ex);
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

  private static void log(String message, Exception ex) {
    PS.println(message);
    if (ex != null) {
      ex.printStackTrace(PS);
    }
  }

  private static void log(String message) {
    log(message, (Exception) null);
  }

  private static void log(String message, Object... args) {
    log(String.format(message, args));
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

  private static final int VERSION_OFFSET = 44;

  private static int vmVersion() {
    String property = System.getProperty("java.class.version", "");
    if (!property.isEmpty()) {
      return (int) (Float.parseFloat(property) - VERSION_OFFSET);
    }
    property = System.getProperty("java.vm.specification.version", "");
    if (property.contains(".")) {
      return (int) Float.parseFloat(property.substring(property.indexOf('.') + 1));
    } else if (!property.isEmpty()) {
      return Integer.parseInt(property);
    }
    log("Fallback vm-version fetch failed, defaulting to 8");
    return 8;
  }
}
