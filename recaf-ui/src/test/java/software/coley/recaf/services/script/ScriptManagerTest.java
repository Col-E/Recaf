package software.coley.recaf.services.script;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableCollection;
import software.coley.recaf.util.IOUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ScriptManager}
 */
class ScriptManagerTest {
	static ScriptManager scriptManager;
	static ObservableBoolean fileWatching;
	static Path scriptDir;

	@BeforeAll
	static void setup() throws IOException {
		// Use our temporary dir for scripts
		scriptDir = Files.createTempDirectory("recaf-scripts");

		// Manage file watching ourselves
		fileWatching = new ObservableBoolean(false);

		ScriptManagerConfig config = mock(ScriptManagerConfig.class);
		when(config.getScriptsDirectory()).thenReturn(scriptDir);
		when(config.getFileWatching()).thenReturn(fileWatching);
		scriptManager = new ScriptManager(config);
	}

	@AfterAll
	static void cleanup() throws IOException {
		fileWatching.setValue(false);
		IOUtil.cleanDirectory(scriptDir);
	}

	@Test
	void testScriptDirectoryScanning() throws InterruptedException {
		ObservableCollection<ScriptFile, List<ScriptFile>> scriptFiles = scriptManager.getScriptFiles();

		// Nothing by default
		assertEquals(0, scriptFiles.size());

		// Enable watching, wait a bit for thread to start
		fileWatching.setValue(true);
		Thread.sleep(50);

		// Add a listener that will assert our desired script is loaded
		String script = """
				// ==Metadata==
				// @name Hello world
				// @description Says hello to the world
				// @version 1.0.0
				// @author Author
				// ==/Metadata==
				    
				System.out.println("Hello world");
				""";
		CompletableFuture<Void> future = new CompletableFuture<>();
		scriptFiles.addChangeListener((ob, old, cur) -> {
			assertEquals(1, scriptFiles.size(), "Script file was not loaded");
			ScriptFile scriptFile = scriptFiles.iterator().next();
			assertEquals("Hello world", scriptFile.name());
			assertEquals("Says hello to the world", scriptFile.description());
			assertEquals("1.0.0", scriptFile.version());
			assertEquals("Author", scriptFile.author());

			// Done
			future.complete(null);
		});

		// Add the script to the directory.
		// Will complete when assertions are passed.
		try {
			Files.writeString(scriptDir.resolve("test.bsh"), script);
			future.get(2, TimeUnit.SECONDS); // Really this is only set for this long because CI can be slow
		} catch (Exception ex) {
			fail(ex);
		}
	}
}
