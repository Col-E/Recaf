package software.coley.recaf.services.workspace.io;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.BasicNativeLibraryFileInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.JarFileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.builtin.ZipAccessTimeProperty;
import software.coley.recaf.info.properties.builtin.ZipCommentProperty;
import software.coley.recaf.info.properties.builtin.ZipCreationTimeProperty;
import software.coley.recaf.info.properties.builtin.ZipMarkerProperty;
import software.coley.recaf.info.properties.builtin.ZipModificationTimeProperty;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.util.ZipCreationUtils;
import software.coley.recaf.util.io.ByteSource;
import software.coley.recaf.util.io.ByteSources;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ResourceImporter}
 */
class ResourceImporterTest {
	static ResourceImporter importer;

	@BeforeAll
	static void setup() {
		importer = new BasicResourceImporter(
				new BasicInfoImporter(new InfoImporterConfig(), new BasicClassPatcher()),
				new ResourceImporterConfig()
		);
	}

	@Test
	void testImportSingleClass() throws IOException {
		JvmClassInfo helloWorldInfo = TestClassUtils.fromRuntimeClass(HelloWorld.class);
		ByteSource classSource = ByteSources.wrap(helloWorldInfo.getBytecode());
		WorkspaceResource resource = importer.importResource(classSource);

		// Should be aware of the class file as its source
		assertInstanceOf(WorkspaceFileResource.class, resource,
				"Resource didn't keep single-class as its associated input");

		// Should have just ONE class in the JVM bundle
		assertEquals(1, resource.getJvmClassBundle().size());
		assertEquals(0, resource.getVersionedJvmClassBundles().size());
		assertEquals(0, resource.getAndroidClassBundles().size());
		assertEquals(0, resource.getFileBundle().size());
		assertEquals(0, resource.getEmbeddedResources().size());
		assertNull(resource.getContainingResource());
		assertFalse(resource.isEmbeddedResource());
		assertFalse(resource.isInternal());

		// Validate JVM class bundle content
		JvmClassInfo classInfo = resource.getJvmClassBundle().iterator().next();
		assertEquals(helloWorldInfo, classInfo, "Missing data compared to baseline input class");
	}

	@Test
	void testImportSingleFile() throws IOException {
		byte[] helloBytes = "Hello".getBytes(StandardCharsets.UTF_8);
		ByteSource classSource = ByteSources.wrap(helloBytes);
		WorkspaceResource resource = importer.importResource(classSource);

		// Should be aware of the class file as its source
		assertInstanceOf(WorkspaceFileResource.class, resource,
				"Resource didn't keep single-file as its associated input");

		// Should have just ONE file in the file bundle
		assertEquals(0, resource.getJvmClassBundle().size());
		assertEquals(0, resource.getVersionedJvmClassBundles().size());
		assertEquals(0, resource.getAndroidClassBundles().size());
		assertEquals(1, resource.getFileBundle().size());
		assertEquals(0, resource.getEmbeddedResources().size());
		assertNull(resource.getContainingResource());
		assertFalse(resource.isEmbeddedResource());
		assertFalse(resource.isInternal());

		// Validate file bundle content
		FileInfo fileInfo = resource.getFileBundle().iterator().next();
		assertArrayEquals(helloBytes, fileInfo.getRawContent(), "Missing data compared to baseline input bytes");
	}

	@Test
	void testImportMultiReleaseVersionedClasses() throws IOException {
		String helloWorldPath = HelloWorld.class.getName().replace(".", "/");
		byte[] helloWorldBytes = TestClassUtils.fromRuntimeClass(HelloWorld.class).getBytecode();

		// Create JAR with 'HelloWorld' declared multiple times in different multi-release directories.
		// None should overlap.
		Map<String, byte[]> map = new LinkedHashMap<>();
		map.put(helloWorldPath + ".class", helloWorldBytes);
		map.put(JarFileInfo.MULTI_RELEASE_PREFIX + "9/" + helloWorldPath + ".class", helloWorldBytes);
		map.put(JarFileInfo.MULTI_RELEASE_PREFIX + "10/" + helloWorldPath + ".class", helloWorldBytes);
		map.put(JarFileInfo.MULTI_RELEASE_PREFIX + "11/" + helloWorldPath + ".class", helloWorldBytes);
		byte[] zipBytes = ZipCreationUtils.createZip(map);
		ByteSource zipSource = ByteSources.wrap(zipBytes);
		WorkspaceResource resource = importer.importResource(zipSource);

		// 1 in the JVM bundle, 3 in each version bundle
		assertEquals(1, resource.getJvmClassBundle().size());
		assertEquals(3, resource.getVersionedJvmClassBundles().size());
		assertEquals(0, resource.getAndroidClassBundles().size());
		assertEquals(0, resource.getFileBundle().size());
		assertEquals(0, resource.getEmbeddedResources().size());

		// Validate class bundle content
		assertArrayEquals(helloWorldBytes, resource.getJvmClassBundle().iterator().next().getBytecode());
		for (JvmClassBundle versioned : resource.getVersionedJvmClassBundles().values()) {
			assertArrayEquals(helloWorldBytes, versioned.iterator().next().getBytecode());
		}
	}

	@Test
	void testImportVersionedClassOnlyWhenNameMatches() throws IOException {
		String helloWorldPath = HelloWorld.class.getName().replace(".", "/");
		byte[] helloWorldBytes = TestClassUtils.fromRuntimeClass(HelloWorld.class).getBytecode();

		// Create JAR with 'HelloWorld' declared twice.
		// Once correctly as a Java 9 versioned class.
		// Once incorrectly as a Java 10 versioned class. This one will be added as a file instead.
		String validName = JarFileInfo.MULTI_RELEASE_PREFIX + "9/" + helloWorldPath + ".class";
		String invalidName = JarFileInfo.MULTI_RELEASE_PREFIX + "10/Bogus.class";
		Map<String, byte[]> map = new LinkedHashMap<>();
		map.put(validName, helloWorldBytes);
		map.put(invalidName, helloWorldBytes);
		byte[] zipBytes = ZipCreationUtils.createZip(map);
		ByteSource zipSource = ByteSources.wrap(zipBytes);
		WorkspaceResource resource = importer.importResource(zipSource);

		// 1 in the JVM bundle, 3 in each version bundle
		assertEquals(0, resource.getJvmClassBundle().size());
		assertEquals(1, resource.getVersionedJvmClassBundles().size());
		assertEquals(0, resource.getAndroidClassBundles().size());
		assertEquals(1, resource.getFileBundle().size());
		assertEquals(0, resource.getEmbeddedResources().size());

		// Validate the added file is the invalid path item
		assertEquals(invalidName, resource.getFileBundle().iterator().next().getName());
	}

	@Test
	void testSupportClassFakeDirectory() throws IOException {
		String helloWorldPath = HelloWorld.class.getName().replace(".", "/");
		byte[] helloWorldBytes = TestClassUtils.fromRuntimeClass(HelloWorld.class).getBytecode();

		// Create JAR with 'HelloWorld' declared but the class file has a trailing '/' in the entry name.
		Map<String, byte[]> map = new LinkedHashMap<>();
		map.put(helloWorldPath + ".class/", helloWorldBytes);
		byte[] zipBytes = ZipCreationUtils.createZip(map);
		ByteSource zipSource = ByteSources.wrap(zipBytes);
		WorkspaceResource resource = importer.importResource(zipSource);

		// 1 in the JVM bundle, 3 in each version bundle
		assertEquals(1, resource.getJvmClassBundle().size());
		assertEquals(0, resource.getVersionedJvmClassBundles().size());
		assertEquals(0, resource.getAndroidClassBundles().size());
		assertEquals(0, resource.getFileBundle().size());
		assertEquals(0, resource.getEmbeddedResources().size());

		// Validate JVM class bundle content
		assertArrayEquals(helloWorldBytes, resource.getJvmClassBundle().iterator().next().getBytecode());
	}

	@Test
	void testAlwaysUseLastClassEntry() throws IOException {
		String helloWorldPath = HelloWorld.class.getName().replace(".", "/");
		byte[] helloWorldBytes = TestClassUtils.fromRuntimeClass(HelloWorld.class).getBytecode();
		byte[] emptyBytes = new byte[0];

		// Create JAR with duplicate entries, with the last entry by the same name containing real data.
		// The first is a red herring and should be ignored, as the JVM does when it encounters repeats.
		byte[] zipBytes = ZipCreationUtils.builder()
				.add(helloWorldPath + ".class", emptyBytes)
				.add(helloWorldPath + ".class", helloWorldBytes)
				.bytes();
		ByteSource zipSource = ByteSources.wrap(zipBytes);
		WorkspaceResource resource = importer.importResource(zipSource);

		// Should only have the one JVM class, the duplicate gets added as a file.
		assertEquals(1, resource.getJvmClassBundle().size());
		assertEquals(0, resource.getVersionedJvmClassBundles().size());
		assertEquals(0, resource.getAndroidClassBundles().size());
		assertEquals(1, resource.getFileBundle().size());
		assertEquals(0, resource.getEmbeddedResources().size());

		// Validate the version chosen is the last one
		assertArrayEquals(helloWorldBytes, resource.getJvmClassBundle().iterator().next().getBytecode());
		assertArrayEquals(emptyBytes, resource.getFileBundle().iterator().next().getRawContent());
	}

	@Test
	void testAlwaysUseLastMultiReleaseClassEntry() throws IOException {
		String helloWorldPath = HelloWorld.class.getName().replace(".", "/");
		byte[] helloWorldBytes = TestClassUtils.fromRuntimeClass(HelloWorld.class).getBytecode();
		byte[] emptyBytes = new byte[0];

		// Create JAR with 'HelloWorld' declared as duplicate entries in multi-release directories.
		byte[] zipBytes = ZipCreationUtils.builder()
				.add(JarFileInfo.MULTI_RELEASE_PREFIX + "9/" + helloWorldPath + ".class", emptyBytes)
				.add(JarFileInfo.MULTI_RELEASE_PREFIX + "9/" + helloWorldPath + ".class", helloWorldBytes)
				.bytes();
		ByteSource zipSource = ByteSources.wrap(zipBytes);
		WorkspaceResource resource = importer.importResource(zipSource);

		// Should only have the one versioned bundle, the duplicate gets added as a file.
		assertEquals(0, resource.getJvmClassBundle().size());
		assertEquals(1, resource.getVersionedJvmClassBundles().size());
		assertEquals(0, resource.getAndroidClassBundles().size());
		assertEquals(1, resource.getFileBundle().size());
		assertEquals(0, resource.getEmbeddedResources().size());

		// Validate the version chosen is the last one
		assertArrayEquals(helloWorldBytes, resource.getVersionedJvmClassBundles().get(9).iterator().next().getBytecode());
		assertArrayEquals(emptyBytes, resource.getFileBundle().iterator().next().getRawContent());
	}

	@Test
	void testAlwaysUseLastFileEntry() throws IOException {
		String path = HelloWorld.class.getName().replace(".", "/");
		byte[] bytes = new byte[]{1, 2, 3};

		// Create JAR with duplicate entries, with the last entry by the same name containing real data.
		// The first is a red herring and should be ignored, as the JVM does when it encounters repeats.
		byte[] zipBytes = ZipCreationUtils.builder()
				.add(path + ".class", new byte[0])
				.add(path + ".class", bytes)
				.bytes();
		ByteSource zipSource = ByteSources.wrap(zipBytes);
		WorkspaceResource resource = importer.importResource(zipSource);

		// Should only have the one file
		assertEquals(0, resource.getJvmClassBundle().size());
		assertEquals(0, resource.getVersionedJvmClassBundles().size());
		assertEquals(0, resource.getAndroidClassBundles().size());
		assertEquals(1, resource.getFileBundle().size());
		assertEquals(0, resource.getEmbeddedResources().size());

		// Validate the version chosen is the last one
		assertArrayEquals(bytes, resource.getFileBundle().iterator().next().getRawContent());
	}

	@Test
	void testDeduplicateClasses() throws IOException {
		String helloWorldPath = HelloWorld.class.getName().replace(".", "/");
		byte[] helloWorldBytes = TestClassUtils.fromRuntimeClass(HelloWorld.class).getBytecode();

		// Create JAR with duplicate entries.
		//  - case 1: Class is first, followed by 'B.class'
		//  - case 2: Class is last, preceded by 'B.class'
		byte[] zipClassFirst = ZipCreationUtils.builder()
				.add(helloWorldPath + ".class", helloWorldBytes)
				.add("software/coley/B.class", helloWorldBytes)
				.add("B.class", helloWorldBytes)
				.bytes();
		byte[] zipClassLast = ZipCreationUtils.builder()
				.add("software/coley/B.class", helloWorldBytes)
				.add("B.class", helloWorldBytes)
				.add(helloWorldPath + ".class", helloWorldBytes)
				.bytes();

		// Both cases should have the same outcome
		for (byte[] zipBytes : Arrays.asList(zipClassFirst, zipClassLast)) {
			ByteSource zipSource = ByteSources.wrap(zipBytes);
			WorkspaceResource resource = importer.importResource(zipSource);

			// Should only have the one JVM class, the duplicate gets added as a fileInfo.
			assertEquals(1, resource.getJvmClassBundle().size());
			assertEquals(0, resource.getVersionedJvmClassBundles().size());
			assertEquals(0, resource.getAndroidClassBundles().size());
			assertEquals(2, resource.getFileBundle().size());
			assertEquals(0, resource.getEmbeddedResources().size());

			// Validate the contents of the file bundle.
			FileInfo fileInfo1 = resource.getFileBundle().get("B.class");
			FileInfo fileInfo2 = resource.getFileBundle().get("software/coley/B.class");
			assertNotNull(fileInfo1, "Deduplication did not copy wrong path class to files bundle: B.class");
			assertNotNull(fileInfo2, "Deduplication did not copy wrong path class to files bundle: software/coley/B.class");
		}
	}

	@Test
	void testDeduplicateVersionedClasses() throws IOException {
		String helloWorldPath = HelloWorld.class.getName().replace(".", "/");
		byte[] helloWorldBytes = TestClassUtils.fromRuntimeClass(HelloWorld.class).getBytecode();

		// Create JAR with duplicate entries.
		//  - case 1: Class is first, followed by 'B.class'
		//  - case 2: Class is last, preceded by 'B.class'
		byte[] zipClassFirst = ZipCreationUtils.builder()
				.add(JarFileInfo.MULTI_RELEASE_PREFIX + "9/" + helloWorldPath + ".class", helloWorldBytes)
				.add(JarFileInfo.MULTI_RELEASE_PREFIX + "9/B.class", helloWorldBytes)
				.add(JarFileInfo.MULTI_RELEASE_PREFIX + "9/software/coley/B.class", helloWorldBytes)
				.bytes();
		byte[] zipClassLast = ZipCreationUtils.builder()
				.add(JarFileInfo.MULTI_RELEASE_PREFIX + "9/software/coley/B.class", helloWorldBytes)
				.add(JarFileInfo.MULTI_RELEASE_PREFIX + "9/B.class", helloWorldBytes)
				.add(JarFileInfo.MULTI_RELEASE_PREFIX + "9/" + helloWorldPath + ".class", helloWorldBytes)
				.bytes();

		// Both cases should have the same outcome
		for (byte[] zipBytes : Arrays.asList(zipClassFirst, zipClassLast)) {
			ByteSource zipSource = ByteSources.wrap(zipBytes);
			WorkspaceResource resource = importer.importResource(zipSource);

			// Should only have the one JVM class, the duplicate gets added as a fileInfo.
			assertEquals(0, resource.getJvmClassBundle().size());
			assertEquals(1, resource.getVersionedJvmClassBundles().size());
			assertEquals(0, resource.getAndroidClassBundles().size());
			assertEquals(2, resource.getFileBundle().size());
			assertEquals(0, resource.getEmbeddedResources().size());

			// Validate the contents of the file bundle.
			FileInfo fileInfo1 = resource.getFileBundle().get(JarFileInfo.MULTI_RELEASE_PREFIX + "9/B.class");
			FileInfo fileInfo2 = resource.getFileBundle().get(JarFileInfo.MULTI_RELEASE_PREFIX + "9/software/coley/B.class");
			assertNotNull(fileInfo1, "Deduplication did not copy wrong path class to files bundle: B.class");
			assertNotNull(fileInfo2, "Deduplication did not copy wrong path class to files bundle: software/coley/B.class");
		}
	}

	@Test
	void testImportZipInsideZip() throws IOException {
		// create a ZIP holding another ZIP
		String insideZipName = "inner.zip";
		String innerDataName = "data";
		byte[] innerData = {1, 2, 3};
		byte[] insideZipBytes = ZipCreationUtils.createSingleEntryZip(innerDataName, innerData);
		byte[] outsideZipBytes = ZipCreationUtils.createSingleEntryZip(insideZipName, insideZipBytes);
		ByteSource classSource = ByteSources.wrap(outsideZipBytes);
		WorkspaceResource resource = importer.importResource(classSource);

		// Should be aware of the ZIP file as its source
		assertInstanceOf(WorkspaceFileResource.class, resource,
				"Resource didn't keep single-file as its associated input");

		// Should create an embedded resource
		assertEquals(1, resource.getEmbeddedResources().size(), "Should have 1 embedded resource");
		WorkspaceFileResource insideZipResource = resource.getEmbeddedResources().get(insideZipName);
		assertNotNull(insideZipResource, "Incorrect embedded resource path, expected: " + insideZipName +
				" got " + insideZipResource.getFileInfo().getName());

		// Should have expected data
		FileInfo innerDotZip = insideZipResource.getFileBundle().get(innerDataName);
		assertNotNull(innerDotZip);
		assertEquals(innerDataName, innerDotZip.getName());
		assertArrayEquals(innerData, innerDotZip.getRawContent());
	}

	@Test
	void testImportsFromDifferentSourcesAreTheSame() throws IOException {
		// Create zip:
		//  - hello.txt
		//  - foo.zip (containing foo)
		//  - bla/bla/bla/HelloWorld.class
		Map<String, byte[]> map = new LinkedHashMap<>();
		map.put("hello.txt", "Hello world".getBytes(StandardCharsets.UTF_8));
		map.put(HelloWorld.class.getName().replace(".", "/") + ".class",
				TestClassUtils.fromRuntimeClass(HelloWorld.class).getBytecode());
		map.put("data.zip", ZipCreationUtils.createSingleEntryZip("foo", new byte[]{1, 2, 3}));
		byte[] zipBytes = ZipCreationUtils.createZip(map);

		// Write to disk temporarily for test duration
		File tempFile = File.createTempFile("recaf", "test.zip");
		Files.write(tempFile.toPath(), zipBytes);
		tempFile.deleteOnExit();

		// Create workspace resources from each kind of input, all sourced from the same content.
		// They should all be equal.
		WorkspaceResource fromByteSource = importer.importResource(ByteSources.wrap(zipBytes));
		WorkspaceResource fromFile = importer.importResource(tempFile);
		WorkspaceResource fromPath = importer.importResource(tempFile.toPath());
		WorkspaceResource fromUri = importer.importResource(tempFile.toURI());
		WorkspaceResource fromUrl = importer.importResource(tempFile.toURI().toURL());
		assertEquals(fromByteSource, fromFile);
		assertEquals(fromByteSource, fromPath);
		assertEquals(fromByteSource, fromUri);
		assertEquals(fromByteSource, fromUrl);
	}

	@Test
	void testSkipDirectories() throws IOException {
		byte[] empty = new byte[0];
		byte[] fileBytes = "data".getBytes(StandardCharsets.UTF_8);
		byte[] zipBytes = ZipCreationUtils.builder()
				.add("com/", empty)
				.add("com/example/", empty)
				.add("com/example/application/", empty)
				.add("com/example/application/Config.txt", fileBytes)
				.bytes();
		WorkspaceResource resource = importer.importResource(ByteSources.wrap(zipBytes));

		// Should have just ONE file in the file bundle
		assertEquals(0, resource.getJvmClassBundle().size());
		assertEquals(0, resource.getVersionedJvmClassBundles().size());
		assertEquals(0, resource.getAndroidClassBundles().size());
		assertEquals(1, resource.getFileBundle().size());
		assertEquals(0, resource.getEmbeddedResources().size());
		assertNull(resource.getContainingResource());
		assertFalse(resource.isEmbeddedResource());
		assertFalse(resource.isInternal());

		// Validate file bundle content
		FileInfo fileInfo = resource.getFileBundle().iterator().next();
		assertArrayEquals(fileBytes, fileInfo.getRawContent(), "Missing data compared to baseline input bytes");
	}

	@Test
	void testZipProperties() throws IOException {
		// Property declarations
		String comment = "comment";
		long timeCreate = 1000000000000L;
		long timeModify = 1200000000000L;
		long timeAccess = 1400000000000L;

		// Create the file
		String name = "com/example/application/Config.txt";
		byte[] data = new byte[]{1, 2, 3, 4, 5};
		byte[] zipBytes = ZipCreationUtils.builder()
				.add(name, data, false, comment, timeCreate, timeModify, timeAccess)
				.bytes();
		WorkspaceResource resource = importer.importResource(ByteSources.wrap(zipBytes));

		// Should have just ONE file in the file bundle
		assertEquals(0, resource.getJvmClassBundle().size());
		assertEquals(0, resource.getVersionedJvmClassBundles().size());
		assertEquals(0, resource.getAndroidClassBundles().size());
		assertEquals(1, resource.getFileBundle().size());
		assertEquals(0, resource.getEmbeddedResources().size());
		assertNull(resource.getContainingResource());
		assertFalse(resource.isEmbeddedResource());
		assertFalse(resource.isInternal());

		// Validate file bundle content
		FileInfo fileInfo = resource.getFileBundle().iterator().next();
		assertArrayEquals(data, fileInfo.getRawContent(), "Missing data compared to baseline input bytes");
		assertEquals(comment, ZipCommentProperty.get(fileInfo), "Missing comment");
		assertEquals(timeCreate, ZipCreationTimeProperty.get(fileInfo), "Missing creation time");
		assertEquals(timeModify, ZipModificationTimeProperty.get(fileInfo), "Missing modification time");
		assertEquals(timeAccess, ZipAccessTimeProperty.get(fileInfo), "Missing access time");
	}

	/** @see InfoImporterTest#testImportFileWithoutZipPrefixHasZipMarkerAssigned() */
	@Test
	void testImportFileWithExeHeaderAsZipIfZipContentsAreValid() throws IOException {
		// Create virtual ZIP with single 'Hello.txt' and suffix the file with a PE header.
		byte[] zipFileBytes = ZipCreationUtils.createSingleEntryZip("Hello.txt", "Hello world".getBytes(StandardCharsets.UTF_8));
		byte[] inputBytes = new byte[4096];
		inputBytes[0] = 0x4D;
		inputBytes[1] = 0x5A;
		System.arraycopy(zipFileBytes, 0, inputBytes, inputBytes.length - zipFileBytes.length, zipFileBytes.length);
		ByteSource exeSource = ByteSources.wrap(inputBytes);

		// When we import the "executable" we should still load the ZIP file contents since it has a ZIP marker
		// and is able to be processed as a ZIP archive.
		WorkspaceResource resource = importer.importResource(exeSource);
		assertTrue(resource.getFileBundle().containsKey("Hello.txt"));
	}
}