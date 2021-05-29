package me.coley.recaf;

import com.strobel.core.Mapping;
import me.coley.recaf.control.headless.HeadlessController;
import me.coley.recaf.mapping.MappingImpl;
import me.coley.recaf.mapping.Mappings;
import me.coley.recaf.workspace.JavaResource;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static me.coley.recaf.util.Log.info;
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

	@Test
	public void remapManifestTest() throws Exception {
		HeadlessController controller = new HeadlessController(null, null);
		controller.setup();
		invokeRun(controller, "loadworkspace " + getClasspathFile("Manifest.jar").normalize().toAbsolutePath());
		assertEquals("Manifest.jar", controller.getWorkspace().getPrimary().getShortName().toString());

		// Check Manifest
		JavaResource primary = controller.getWorkspace().getPrimary();
		byte[] manifestBytes = primary.getFiles().get("META-INF/MANIFEST.MF");
		assertNotEquals(manifestBytes, null);
		Manifest manifest = new Manifest(new ByteArrayInputStream(manifestBytes));
		Attributes attr = manifest.getMainAttributes();
		assertEquals(attr.size(), 2);
		String mainClass = attr.getValue("Main-Class").replaceAll("\\.", "/");
		assertEquals(mainClass, "Code/Code/Code");

		invokeRun(controller, "remap PROGUARD " + getClasspathFile("manifest-map.txt").normalize().toAbsolutePath());
		manifestBytes = primary.getFiles().get("META-INF/MANIFEST.MF");
		manifest = new Manifest(new ByteArrayInputStream(manifestBytes));
		attr = manifest.getMainAttributes();
		mainClass = attr.getValue("Main-Class").replaceAll("\\.", "/");
		assertEquals(mainClass, "some/pkg/Main");
	}

	private static void invokeRun(HeadlessController controller, String cmd) throws Exception {
		Method m = controller.getClass().getDeclaredMethod("handle", String.class);
		m.setAccessible(true);
		m.invoke(controller, cmd);
	}
}
