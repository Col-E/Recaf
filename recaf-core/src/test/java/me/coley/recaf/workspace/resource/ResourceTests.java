package me.coley.recaf.workspace.resource;

import me.coley.recaf.TestUtils;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.workspace.resource.source.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.PrintStream;
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
	void testResourcesDexClassLookup() throws IOException {
		Resource primary = new Resource(new ApkContentSource(sourcesDir.resolve("Sample.apk")));
		primary.read();
		// Validate the classes exist
		assertTrue(primary.getDexClasses().containsKey("com/example/android/contactmanager/ContactAdder"));
		// Populate resources wrapper and attempt lookup
		Resources resources = new Resources(primary, Collections.emptyList());
		DexClassInfo classFood = resources.getDexClass("com/example/android/contactmanager/ContactAdder");
		FileInfo classHello = resources.getFile("AndroidManifest.xml");
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
		assertEquals(SingleFileContentSource.class,
				ResourceIO.fromPath(sourcesDir.resolve("Sample.class"), false).getContentSource().getClass());
		assertEquals(ApkContentSource.class,
				ResourceIO.fromPath(sourcesDir.resolve("Sample.apk"), false).getContentSource().getClass());
		assertEquals(UrlContentSource.class,
				ResourceIO.fromUrl(sourcesDir.resolve("Sample.jar").toUri().toURL(), false).getContentSource().getClass());
	}

	@Test
	void testRuntimeLookupFindsClass() {
		String typePrintStream = PrintStream.class.getName().replace('.', '/');
		//
		RuntimeResource runtime = RuntimeResource.get();
		ClassInfo info = runtime.getClasses().get(typePrintStream);
		//
		assertEquals(typePrintStream, info.getName());
		assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, info.getAccess());
		assertEquals("java/io/FilterOutputStream", info.getSuperName());
		assertTrue(info.getInterfaces().contains("java/lang/Appendable"));
		assertTrue(info.getInterfaces().contains("java/io/Closeable"));
	}

	@Test
	void testRuntimeLookupIgnoresBogus() {
		String bogus = ";[-=.;'[";
		//
		RuntimeResource runtime = RuntimeResource.get();
		ClassInfo info = runtime.getClasses().get(bogus);
		//
		assertNull(info);
	}
}
