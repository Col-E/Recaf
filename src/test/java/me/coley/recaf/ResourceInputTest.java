package me.coley.recaf;

import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for using {@link JavaResource} implementations.
 *
 * @author Matt
 */
public class ResourceInputTest extends Base {
	private final static int CLASSES_IN_INHERIT_JAR = 9;

	@Test
	public void testJar() {
		try {
			File file = getClasspathFile("inherit.jar");
			JavaResource resource = new JarResource(file);
			assertEquals(CLASSES_IN_INHERIT_JAR, resource.getClasses().size());
		} catch(IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testJarResourcesDoNotContainClasses() {
		try {
			File file = getClasspathFile("calc.jar");
			JavaResource resource = new JarResource(file);
			for (String name : resource.getResources().keySet())
				assertTrue(!name.endsWith(".class"));
		} catch(IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testClass() {
		try {
			File file = getClasspathFile("Hello.class");
			JavaResource resource = new ClassResource(file);
			assertEquals(1, resource.getClasses().size());
		} catch(IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testUrlJar() {
		URL url = getClasspathUrl("inherit.jar");
		JavaResource resource = new UrlResource(url);
		assertEquals(CLASSES_IN_INHERIT_JAR, resource.getClasses().size());
	}

	@Test
	public void testUrlClass() {
		URL url = getClasspathUrl("Hello.class");
		JavaResource resource = new UrlResource(url);
		assertEquals(1, resource.getClasses().size());
	}

	@Test
	public void testMaven() {
		// ASM-commons as of 7.2-beta has 27 classes
		JavaResource resource = new MavenResource("org.ow2.asm","asm-commons","7.2-beta");
		assertEquals(27, resource.getClasses().size());
	}

	@Test
	public void testSkipPrefixes() {
		try {
			File file = getClasspathFile("calc.jar");
			JavaResource resource = new JarResource(file);
			resource.setSkippedPrefixes(Collections.singletonList("calc"));
			assertEquals(1, resource.getClasses().size());
		} catch(IOException ex) {
			fail(ex);
		}
	}

	// ================== BAD INPUTS ====================== //

	@Test
	public void testUrlClassDoesNotExist() {
		try {
			URL url = new URL("file://DoesNotExist.class");
			assertThrows(IllegalArgumentException.class, () -> new UrlResource(url));
		} catch(MalformedURLException ex) {
			fail(ex);
		}
	}

	@Test
	public void testFileDoesNotExist() {
		File file = new File("DoesNotExist.class");
		assertThrows(IllegalArgumentException.class, () -> new ClassResource(file));
	}

	@Test
	public void testMavenDoesNotExist() {
		assertThrows(IllegalArgumentException.class, () -> new MavenResource("does","not","exist"));
	}
}
