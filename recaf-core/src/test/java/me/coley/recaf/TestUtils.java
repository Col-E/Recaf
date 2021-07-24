package me.coley.recaf;

import org.junit.jupiter.api.Assumptions;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Common test utilities.
 */
public class TestUtils {
	protected static final Path resourcesDir = getResourcesPath();
	protected static final Path sourcesDir = getResourcesPath().resolve("content-sources");
	protected static final Path jarsDir = getResourcesPath().resolve("jars");

	private static Path getResourcesPath() {
		Path currentDir = Paths.get(System.getProperty("user.dir"));
		if (Files.exists(currentDir.resolve("src"))) {
			return Paths.get("src", "test", "resources");
		} else {
			return Paths.get("recaf-core", "src", "test", "resources");
		}
	}

	protected static void assumeUrlExists(String urlPath) {
		try {
			URL url = new URL(urlPath);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("HEAD");
			int response = connection.getResponseCode();
			Assumptions.assumeTrue(response >= 200 && response <= 299);
		} catch (Exception ex) {
			Assumptions.assumeFalse(true);
		}
	}
}
