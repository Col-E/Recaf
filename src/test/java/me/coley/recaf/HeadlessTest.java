package me.coley.recaf;

import me.coley.recaf.control.headless.HeadlessController;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for basic functionality of headless mode.
 *
 * @author Matt
 */
public class HeadlessTest extends Base {
	@Test
	public void test() throws Exception {
		HeadlessController controller = new HeadlessController(null, null);
		controller.setup();
		invokeRun(controller, "help");
		invokeRun(controller, "loadworkspace " + getClasspathFile("calc.jar").normalize().toAbsolutePath());
		assertEquals("calc.jar", controller.getWorkspace().getPrimary().getShortName().toString());
	}

	private static void invokeRun(HeadlessController controller, String cmd) throws Exception {
		Method m = controller.getClass().getDeclaredMethod("handle", String.class);
		m.setAccessible(true);
		m.invoke(controller, cmd);
	}
}
