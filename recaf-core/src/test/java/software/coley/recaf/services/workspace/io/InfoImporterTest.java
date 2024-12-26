package software.coley.recaf.services.workspace.io;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.*;
import software.coley.recaf.info.properties.builtin.ZipMarkerProperty;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.util.ZipCreationUtils;
import software.coley.recaf.util.io.ByteSource;
import software.coley.recaf.util.io.ByteSources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link InfoImporter}
 */
class InfoImporterTest {
	static final ClassPatcher mockedPatcher = mock(ClassPatcher.class);
	static InfoImporter importer;

	@BeforeAll
	static void setup() {
		importer = new BasicInfoImporter(new InfoImporterConfig(), mockedPatcher);
	}

	@Test
	void testImportRuntimeClassesAsJvmClasses() throws IOException {
		for (Class<?> type : Arrays.asList(Collection.class, List.class, ArrayList.class)) {
			// Fetch class from runtime.
			JvmClassInfo classInfo = TestClassUtils.fromRuntimeClass(type);

			// Pass the class's bytecode into the importer and see what we get back.
			ByteSource bytecodeSource = ByteSources.wrap(classInfo.getBytecode());
			Info read = importer.readInfo(classInfo.getName(), bytecodeSource);

			// Should be a JVM class
			assertTrue(read.isClass());
			ClassInfo readClass = read.asClass();
			assertTrue(readClass.isJvmClass());
			JvmClassInfo readJvmClass = readClass.asJvmClass();

			// Should have equality with the original
			assertEquals(classInfo, readJvmClass, "Class from importer does not match class from runtime");
		}
	}

	@Test
	void testUsesClassPatcher() throws IOException {
		JvmClassInfo classInfo = TestClassUtils.fromRuntimeClass(ArrayList.class);
		ByteSource invalidSource = ByteSources.wrap(classInfo.getBytecode(), 0, 20);

		// Cut off the class file so that it is 'invalid'.
		// We've passed in a no-op mock for the patcher, so it won't do anything currently.
		Info read = importer.readInfo("", invalidSource);
		assertFalse(read.isClass(), "Should have failed to read class from cut off data");
		assertTrue(read.isFile(), "Should have read partial class as generic file");

		// Mock it so that patcher 'fixes' the bytecode by returning the original.
		when(mockedPatcher.patch(anyString(), any())).thenReturn(classInfo.getBytecode());

		// Now that patcher component is 'implemented' the class should be fixable by the importer.
		read = importer.readInfo("", invalidSource);
		assertTrue(read.isClass(), "Should have used patcher to fix invalid class bytecode");
		assertTrue(read.asClass().isJvmClass(), "Created wrong class type");
	}

	@Test
	void testImportEmptyArrayAsGenericFile() throws IOException {
		// Importing something that cannot be identified, should use 'FileInfo' as catch-all.
		Info read = importer.readInfo("", ByteSources.wrap(new byte[0]));
		assertTrue(read.isFile());

		// Specifically, because we don't know anything about the file, we should not assign it to
		// anything more detailed than this.
		assertEquals(BasicFileInfo.class, read.getClass());
	}

	@Test
	void testImportZip() throws IOException {
		// Create virtual ZIP with single 'Hello.txt'
		byte[] zipFileBytes = ZipCreationUtils.createSingleEntryZip("Hello.txt", "Hello world".getBytes(StandardCharsets.UTF_8));
		ByteSource zipSource = ByteSources.wrap(zipFileBytes);

		// We don't know the file name, so we can only assume it is a ZIP
		Info read = importer.readInfo("", zipSource);
		assertTrue(read.isFile());
		assertTrue(read.asFile().isZipFile());
		assertEquals(BasicZipFileInfo.class, read.getClass());
		ZipFileInfo readZip = read.asFile().asZipFile();
		assertArrayEquals(zipFileBytes, readZip.getRawContent());

		// However, if we provide various extensions then we can use the file name to infer what kind of ZIP it is.
		read = importer.readInfo("data.jar", zipSource);
		assertInstanceOf(JarFileInfo.class, read);
		read = importer.readInfo("data.war", zipSource);
		assertInstanceOf(WarFileInfo.class, read);
		read = importer.readInfo("data.jmod", zipSource);
		assertInstanceOf(JModFileInfo.class, read);
		read = importer.readInfo("data.apk", zipSource);
		assertInstanceOf(ApkFileInfo.class, read);
	}

	/** @see ResourceImporterTest#testImportFileWithExeHeaderAsZipIfZipContentsAreValid() */
	@Test
	void testImportFileWithoutZipPrefixHasZipMarkerAssigned() throws IOException {
		// Create virtual ZIP with single 'Hello.txt' and suffix the file with a PE header.
		byte[] zipFileBytes = ZipCreationUtils.createSingleEntryZip("Hello.txt", "Hello world".getBytes(StandardCharsets.UTF_8));
		byte[] inputBytes = new byte[4096];
		inputBytes[0] = 0x4D;
		inputBytes[1] = 0x5A;
		System.arraycopy(zipFileBytes, 0, inputBytes, inputBytes.length - zipFileBytes.length, zipFileBytes.length);
		ByteSource exeSource = ByteSources.wrap(inputBytes);

		// We should import the info as an executable since the header matches
		// but note that it has the ZIP marker in its contents.
		Info read = importer.readInfo("example.exe", exeSource);
		assertTrue(read.isFile());
		assertTrue(read.asFile().isNativeLibraryFile());
		assertFalse(read.asFile().isZipFile());
		assertEquals(BasicNativeLibraryFileInfo.class, read.getClass());

		// The ZIP marker should exist.
		assertTrue(ZipMarkerProperty.get(read.asFile()));
	}
}