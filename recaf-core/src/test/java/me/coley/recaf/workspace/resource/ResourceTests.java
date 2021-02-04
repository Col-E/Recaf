package me.coley.recaf.workspace.resource;

import me.coley.recaf.TestUtils;
import me.coley.recaf.workspace.resource.source.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for resource handling.
 */
public class ResourceTests extends TestUtils {
	@Test
	void testResourcesClassLookup() throws IOException {
		Resource primary = new Resource(new JarContentSource(sourcesDir.resolve("Sample.jar")));
		Resource secondary = new Resource(new WarContentSource(sourcesDir.resolve("Sample.war")));
		primary.read();
		secondary.read();
		// Validate the classes exist
		assertTrue(primary.getClasses().containsKey("game/Food"));
		assertTrue(secondary.getClasses().containsKey("mypackage/Hello"));
		// Populate resources wrapper and attempt lookup
		Resources resources = new Resources(primary, Collections.singletonList(secondary));
		ClassInfo classFood = resources.getClass("game/Food");
		ClassInfo classHello = resources.getClass("mypackage/Hello");
		assertNotNull(classFood);
		assertNotNull(classHello);
	}

	@Test
	void testResourcesFileLookup() throws IOException {
		Resource primary = new Resource(new WarContentSource(sourcesDir.resolve("Sample.war")));
		primary.read();
		// Validate the file exists
		assertTrue(primary.getFiles().containsKey("hello.jsp"));
		// Populate resources wrapper and attempt lookup
		Resources resources = new Resources(primary);
		FileInfo fileHello = resources.getFile("hello.jsp");
		assertNotNull(fileHello);
	}

	@Test
	void testResourceIO() throws IOException {
		assertEquals(DirectoryContentSource.class,
				ResourceIO.fromPath(sourcesDir, false).getContentSource().getClass());
		assertEquals(JarContentSource.class,
				ResourceIO.fromPath(sourcesDir.resolve("Sample.jar"), false).getContentSource().getClass());
		assertEquals(ZipContentSource.class,
				ResourceIO.fromPath(sourcesDir.resolve("Sample.zip"), false).getContentSource().getClass());
		assertEquals(WarContentSource.class,
				ResourceIO.fromPath(sourcesDir.resolve("Sample.war"), false).getContentSource().getClass());
		assertEquals(ClassContentSource.class,
				ResourceIO.fromPath(sourcesDir.resolve("Sample.class"), false).getContentSource().getClass());
		assertEquals(UrlContentSource.class,
				ResourceIO.fromUrl(sourcesDir.resolve("Sample.jar").toUri().toURL().toString(), false).getContentSource().getClass());

	}
}
