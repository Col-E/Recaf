package me.coley.recaf.util;

import me.coley.recaf.Recaf;
import me.coley.recaf.control.Controller;
import me.coley.recaf.control.headless.HeadlessController;
import me.coley.recaf.workspace.JavaResource;
import me.coley.recaf.workspace.Workspace;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Some common utilities.
 */
public class TestUtils {
	/**
	 * Set the current controller to a wrapper of the given resource.
	 *
	 * @param resource
	 * 		Resource to wrap in a headless controller.
	 *
	 * @return Controller instance.
	 *
	 * @throws IOException
	 * 		When the config cannot be initialized.
	 */
	public static Controller setupController(JavaResource resource) throws IOException {
		// Set up the controller
		Controller controller = new HeadlessController((Path) null, null);
		controller.setWorkspace(new Workspace(resource));
		controller.config().initialize();
		return controller;
	}

	/**
	 * Used reflection to remove the controller...
	 */
	public static void removeController() {
		try {
			// In "Recaf.java" we prevent setting the controller twice...
			// I swear I'm taking crazy pills, because the surefire config should be isolating tests...
			// That means each test should get a separate JVM. But clearly something is wrong.
			Field f = Recaf.class.getDeclaredField("currentController");
			f.setAccessible(true);
			f.set(null, null);
		} catch(Exception ex) {
			fail("Failed to reset");
		}
	}
}
