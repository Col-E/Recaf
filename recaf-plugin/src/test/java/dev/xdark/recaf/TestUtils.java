package dev.xdark.recaf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Common test utilities.
 */
public class TestUtils {
	protected static final Path sourcesDir = getResourcesPath().resolve("content-sources");

	private static Path getResourcesPath() {
		Path currentDir = Paths.get(System.getProperty("user.dir"));
		if (Files.exists(currentDir.resolve("src"))) {
			return Paths.get("src", "test", "resources");
		} else {
			return Paths.get("recaf-ui", "src", "test", "resources");
		}
	}
}
