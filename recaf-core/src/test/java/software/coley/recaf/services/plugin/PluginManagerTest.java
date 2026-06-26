package software.coley.recaf.services.plugin;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import software.coley.recaf.plugin.*;
import software.coley.recaf.services.plugin.discovery.DiscoveredPluginSource;
import software.coley.recaf.services.plugin.discovery.PluginDiscoverer;
import software.coley.recaf.services.plugin.zip.ZipPluginLoader;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.util.ZipCreationUtils;
import software.coley.recaf.util.io.ByteSource;
import software.coley.recaf.util.io.ByteSources;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Tests for {@link PluginManager}
 */
public class PluginManagerTest extends TestBase {
	static PluginManager pluginManager;

	@BeforeAll
	static void setup() {
		pluginManager = recaf.get(PluginManager.class);
	}

	@AfterEach
	void verifyCleanSlate() {
		assertEquals(0, pluginManager.getPlugins().size(), "Plugins still loaded after test case");
	}

	@Test
	void testBatchLoadedPluginCanReferenceDependencyClasses() throws IOException {
		String apiId = "api-plugin";
		String implId = "impl-plugin";

		String apiPluginClass = "test/ApiPlugin";
		String apiTypeClass = "test/ApiType";
		String implPluginClass = "test/ImplPlugin";

		byte[] apiZip = createPluginZip(apiPluginClass, createPluginClass(apiPluginClass, apiId, new String[0]), Map.of(
				apiTypeClass + ".class", createStaticStringProviderClass(apiTypeClass, "message", "dependency-ok")
		));

		byte[] implZip = createPluginZip(implPluginClass, createPluginClassCallingDependencyStringProvider(
				implPluginClass,
				implId,
				new String[]{apiId},
				apiTypeClass,
				"message",
				"dependency-ok"
		), Map.of());

		PluginDiscoverer discoverer = () -> List.of(
				() -> ByteSources.wrap(apiZip),
				() -> ByteSources.wrap(implZip)
		);

		try {
			Collection<PluginContainer<?>> containers = pluginManager.loadPlugins(discoverer);

			assertEquals(2, containers.size());
			assertNotNull(pluginManager.getPlugin(apiId));
			assertNotNull(pluginManager.getPlugin(implId));

			pluginManager.unloaderFor(apiId).commit();

			assertNull(pluginManager.getPlugin(apiId));
			assertNull(pluginManager.getPlugin(implId));
		} catch (PluginException ex) {
			fail("Failed to load plugins with same-batch dependency class visibility", ex);
		}
	}

	@Test
	void testPluginResourcesAreVisibleThroughGetResources() throws IOException {
		String id = "resource-enumeration-plugin";
		String pluginClass = "test/ResourceEnumerationPlugin";
		String resourceName = "META-INF/services/example.Service";
		String resourceContent = "example.Implementation";

		byte[] zip = createPluginZip(pluginClass, createPluginClassCallingEnumeratedResourceAssertion(
				pluginClass,
				id,
				resourceName,
				resourceContent
		), Map.of(
				resourceName, resourceContent.getBytes(StandardCharsets.UTF_8)
		));

		PluginDiscoverer discoverer = () -> List.of(() -> ByteSources.wrap(zip));

		try {
			pluginManager.loadPlugins(discoverer);
			pluginManager.unloaderFor(id).commit();
		} catch (PluginException ex) {
			fail("Failed to enumerate plugin resources", ex);
		}
	}

	@Test
	void testPluginResourceUrlsIncludePluginId() throws IOException {
		String id = "scoped-resource-plugin";
		String pluginClass = "test/ScopedResourcePlugin";
		String resourceName = "plugin-resource.txt";

		byte[] zip = createPluginZip(pluginClass, createPluginClassCallingResourceUrlScopeAssertion(
				pluginClass,
				id,
				resourceName,
				id
		), Map.of(
				resourceName, "scoped".getBytes(StandardCharsets.UTF_8)
		));

		PluginDiscoverer discoverer = () -> List.of(() -> ByteSources.wrap(zip));

		try {
			pluginManager.loadPlugins(discoverer);
			pluginManager.unloaderFor(id).commit();
		} catch (PluginException ex) {
			fail("Failed to validate plugin resource URL scope", ex);
		}
	}

	@Test
	void testPluginResourceUrlReturnsFreshStreams() throws IOException {
		String id = "fresh-stream-plugin";
		String pluginClass = "test/FreshStreamPlugin";
		String resourceName = "fresh-resource.txt";
		String resourceContent = "fresh-content";

		byte[] zip = createPluginZip(pluginClass, createPluginClassCallingFreshResourceStreamAssertion(
				pluginClass,
				id,
				resourceName,
				resourceContent
		), Map.of(
				resourceName, resourceContent.getBytes(StandardCharsets.UTF_8)
		));

		PluginDiscoverer discoverer = () -> List.of(() -> ByteSources.wrap(zip));

		try {
			pluginManager.loadPlugins(discoverer);
			pluginManager.unloaderFor(id).commit();
		} catch (PluginException ex) {
			fail("Failed to validate fresh plugin resource streams", ex);
		}
	}

	public static void assertSameText(String expected, String actual) {
		assertEquals(expected, actual);
	}

	public static void assertEnumeratedResource(ClassLoader classLoader, String name, String expectedContent) throws IOException {
		Enumeration<URL> resources = classLoader.getResources(name);

		assertTrue(resources.hasMoreElements(), "Expected resource to be visible through ClassLoader#getResources: " + name);

		URL resource = resources.nextElement();
		assertNotNull(resource, "Enumerated resource URL must not be null");

		try (InputStream inputStream = resource.openStream()) {
			assertEquals(expectedContent, new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	public static void assertResourceUrlContains(ClassLoader classLoader, String name, String expectedUrlPart) {
		URL resource = classLoader.getResource(name);

		assertNotNull(resource, "Expected resource URL: " + name);
		assertTrue(resource.toExternalForm().contains(expectedUrlPart),
				"Expected resource URL to contain '%s', got: %s".formatted(expectedUrlPart, resource));
	}

	public static void assertFreshResourceStreams(ClassLoader classLoader, String name, String expectedContent) throws IOException {
		URL resource = classLoader.getResource(name);

		assertNotNull(resource, "Expected resource URL: " + name);

		URLConnection connection = resource.openConnection();

		String firstRead;
		try (InputStream inputStream = connection.getInputStream()) {
			firstRead = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}

		String secondRead;
		try (InputStream inputStream = connection.getInputStream()) {
			secondRead = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}

		assertEquals(expectedContent, firstRead);
		assertEquals(expectedContent, secondRead);
	}

	private static byte[] createPluginZip(String pluginInternalName, byte[] pluginClassBytes, Map<String, byte[]> additionalEntries) throws IOException {
		Map<String, byte[]> entries = new LinkedHashMap<>();
		entries.put(pluginInternalName + ".class", pluginClassBytes);
		entries.put(ZipPluginLoader.SERVICE_PATH, pluginInternalName.replace('/', '.').getBytes(StandardCharsets.UTF_8));
		entries.putAll(additionalEntries);
		return ZipCreationUtils.createZip(entries);
	}

	private static byte[] createPluginClass(String internalName, String id, String[] dependencies) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cw.visit(V22, ACC_PUBLIC | ACC_SUPER, internalName, null, "java/lang/Object", new String[]{"software/coley/recaf/plugin/Plugin"});
		visitPluginInformation(cw, id, dependencies);
		writeDefaultConstructor(cw);
		writeEmptyMethod(cw, "onEnable");
		writeEmptyMethod(cw, "onDisable");
		cw.visitEnd();
		return cw.toByteArray();
	}

	private static byte[] createPluginClassCallingDependencyStringProvider(
			String internalName,
			String id,
			String[] dependencies,
			String providerInternalName,
			String providerMethodName,
			String expectedValue
	) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cw.visit(V22, ACC_PUBLIC | ACC_SUPER, internalName, null, "java/lang/Object", new String[]{"software/coley/recaf/plugin/Plugin"});
		visitPluginInformation(cw, id, dependencies);
		writeDefaultConstructor(cw);

		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "onEnable", "()V", null, null);
		mv.visitCode();
		mv.visitLdcInsn(expectedValue);
		mv.visitMethodInsn(INVOKESTATIC, providerInternalName, providerMethodName, "()Ljava/lang/String;", false);
		mv.visitMethodInsn(INVOKESTATIC,
				"software/coley/recaf/services/plugin/PluginManagerTest",
				"assertSameText",
				"(Ljava/lang/String;Ljava/lang/String;)V",
				false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		writeEmptyMethod(cw, "onDisable");
		cw.visitEnd();
		return cw.toByteArray();
	}

	private static byte[] createPluginClassCallingEnumeratedResourceAssertion(
			String internalName,
			String id,
			String resourceName,
			String expectedContent
	) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cw.visit(V22, ACC_PUBLIC | ACC_SUPER, internalName, null, "java/lang/Object", new String[]{"software/coley/recaf/plugin/Plugin"});
		visitPluginInformation(cw, id, new String[0]);
		writeDefaultConstructor(cw);

		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "onEnable", "()V", null, null);
		mv.visitCode();
		writeCurrentClassLoader(mv);
		mv.visitLdcInsn(resourceName);
		mv.visitLdcInsn(expectedContent);
		mv.visitMethodInsn(INVOKESTATIC,
				"software/coley/recaf/services/plugin/PluginManagerTest",
				"assertEnumeratedResource",
				"(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/String;)V",
				false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		writeEmptyMethod(cw, "onDisable");
		cw.visitEnd();
		return cw.toByteArray();
	}

	private static byte[] createPluginClassCallingResourceUrlScopeAssertion(
			String internalName,
			String id,
			String resourceName,
			String expectedUrlPart
	) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cw.visit(V22, ACC_PUBLIC | ACC_SUPER, internalName, null, "java/lang/Object", new String[]{"software/coley/recaf/plugin/Plugin"});
		visitPluginInformation(cw, id, new String[0]);
		writeDefaultConstructor(cw);

		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "onEnable", "()V", null, null);
		mv.visitCode();
		writeCurrentClassLoader(mv);
		mv.visitLdcInsn(resourceName);
		mv.visitLdcInsn(expectedUrlPart);
		mv.visitMethodInsn(INVOKESTATIC,
				"software/coley/recaf/services/plugin/PluginManagerTest",
				"assertResourceUrlContains",
				"(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/String;)V",
				false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		writeEmptyMethod(cw, "onDisable");
		cw.visitEnd();
		return cw.toByteArray();
	}

	private static byte[] createPluginClassCallingFreshResourceStreamAssertion(
			String internalName,
			String id,
			String resourceName,
			String expectedContent
	) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cw.visit(V22, ACC_PUBLIC | ACC_SUPER, internalName, null, "java/lang/Object", new String[]{"software/coley/recaf/plugin/Plugin"});
		visitPluginInformation(cw, id, new String[0]);
		writeDefaultConstructor(cw);

		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "onEnable", "()V", null, null);
		mv.visitCode();
		writeCurrentClassLoader(mv);
		mv.visitLdcInsn(resourceName);
		mv.visitLdcInsn(expectedContent);
		mv.visitMethodInsn(INVOKESTATIC,
				"software/coley/recaf/services/plugin/PluginManagerTest",
				"assertFreshResourceStreams",
				"(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/String;)V",
				false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		writeEmptyMethod(cw, "onDisable");
		cw.visitEnd();
		return cw.toByteArray();
	}

	private static byte[] createStaticStringProviderClass(String internalName, String methodName, String value) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cw.visit(V22, ACC_PUBLIC | ACC_SUPER, internalName, null, "java/lang/Object", null);
		writeDefaultConstructor(cw);

		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, methodName, "()Ljava/lang/String;", null, null);
		mv.visitCode();
		mv.visitLdcInsn(value);
		mv.visitInsn(ARETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		cw.visitEnd();
		return cw.toByteArray();
	}

	private static void visitPluginInformation(ClassWriter cw, String id, String[] dependencies) {
		AnnotationVisitor av = cw.visitAnnotation("Lsoftware/coley/recaf/plugin/PluginInformation;", true);
		av.visit("id", id);
		av.visit("name", id);
		av.visit("version", "1.0");

		if (dependencies.length > 0) {
			AnnotationVisitor dependenciesVisitor = av.visitArray("dependencies");
			for (String dependency : dependencies)
				dependenciesVisitor.visit(null, dependency);
			dependenciesVisitor.visitEnd();
		}

		av.visitEnd();
	}

	private static void writeDefaultConstructor(ClassWriter cw) {
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	private static void writeEmptyMethod(ClassWriter cw, String name) {
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, name, "()V", null, null);
		mv.visitCode();
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	private static void writeCurrentClassLoader(MethodVisitor mv) {
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
	}

	@Test
	void testSingleLoadAndUnload() throws IOException {
		String id = "test-plugin";
		String name = id;
		String version = "test-version";
		String author = "test-author";
		String description = "test-description";
		String className = "test.PluginTest";
		DynamicType.Unloaded<Plugin> unloaded = new ByteBuddy()
				.subclass(Plugin.class)
				.name(className)
				.defineMethod("onEnable", void.class, Modifier.PUBLIC)
				.intercept(FixedValue.originType())
				.defineMethod("onDisable", void.class, Modifier.PUBLIC)
				.intercept(FixedValue.originType())
				.annotateType(new PluginInformationRecord(id, name, version, author, new String[0], description)).make();
		byte[] zip = ZipCreationUtils.createZip(Map.of(
				"test/PluginTest.class", unloaded.getBytes(),
				ZipPluginLoader.SERVICE_PATH, className.getBytes(StandardCharsets.UTF_8)
		));

		try {
			// Load the plugin
			ByteSource pluginSource = ByteSources.wrap(zip);
			PluginDiscoverer discoverer = () -> List.of(() -> pluginSource);
			PluginContainer<?> container = pluginManager.loadPlugins(discoverer).iterator().next();

			// Assert the information stuck
			PluginInfo information = container.info();
			assertEquals(id, information.id());
			assertEquals(name, information.name());
			assertEquals(version, information.version());
			assertEquals(author, information.author());
			assertEquals(description, information.description());

			// Assert the plugin is active
			assertEquals(1, pluginManager.getPlugins().size());
			assertSame(container, pluginManager.getPlugin(id));

			// Assert that loading the same plugin twice throws an exception, and does
			// not actually register a 2nd instance of the plugin.
			assertThrows(PluginException.class, () -> pluginManager.loadPlugins(discoverer),
					"Duplicate plugin loading should fail");
			assertEquals(1, pluginManager.getPlugins().size());
			assertSame(container, pluginManager.getPlugin(id));

			// Now unload it
			pluginManager.unloaderFor(id).commit();

			// Assert the plugin is no longer active
			assertEquals(0, pluginManager.getPlugins().size());
			assertNull(pluginManager.getPlugin(id));
		} catch (PluginException ex) {
			fail("Failed to load plugin", ex);
		}
	}

	@Test
	void testDependentChain() throws IOException {
		List<DiscoveredPluginSource> sources = new ArrayList<>(3);
		for (int i = 0; i < 3; i++) {
			String id = "test-plugin-" + i;
			String name = id;
			String version = "test-version";
			String author = "test-author";
			String description = "test-description";
			String className = "test.PluginTest" + i;
			String[] dependencies = i > 0 ? new String[]{"test-plugin-" + (i - 1)} : new String[0];
			DynamicType.Unloaded<Plugin> unloaded = new ByteBuddy()
					.subclass(Plugin.class)
					.name(className)
					.defineMethod("onEnable", void.class, Modifier.PUBLIC)
					.intercept(FixedValue.originType())
					.defineMethod("onDisable", void.class, Modifier.PUBLIC)
					.intercept(FixedValue.originType())
					.annotateType(new PluginInformationRecord(id, name, version, author, dependencies, description)).make();
			byte[] zip = ZipCreationUtils.createZip(Map.of(
					"test/PluginTest" + i + ".class", unloaded.getBytes(),
					ZipPluginLoader.SERVICE_PATH, className.getBytes(StandardCharsets.UTF_8)
			));
			sources.add(() -> ByteSources.wrap(zip));
		}


		try {
			// Load the plugin
			PluginDiscoverer discoverer = () -> sources;
			pluginManager.loadPlugins(discoverer);

			// Assert the plugins are active
			assertEquals(3, pluginManager.getPlugins().size());

			// Now unload them
			pluginManager.unloaderFor("test-plugin-0").commit();

			// Assert the plugins are no longer active
			assertEquals(0, pluginManager.getPlugins().size());
			for (int i = 0; i < 3; i++) {
				String id = "test-plugin-" + i;
				assertNull(pluginManager.getPlugin(id));
			}
		} catch (PluginException ex) {
			fail("Failed to load plugins", ex);
		}
	}

	@Test
	void testPluginWithResourceLoading() throws IOException {
		String className = "DummyPluginBody";
		byte[] classBytes;
		{
			/*
			@PluginInformation(id = "dummy", name = "dummy", version = "1.0")
			public class DummyPluginBody implements Plugin {
				@Override
				public void onEnable() {
					try (var s = getClass().getResourceAsStream("file.txt")) {
						System.out.println(new String(s.readAllBytes()));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				@Override
				public void onDisable() {}
			}
			 */

			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
			cw.visit(V22, ACC_PUBLIC | ACC_SUPER, className, null, "java/lang/Object", new String[]{"software/coley/recaf/plugin/Plugin"});
			AnnotationVisitor av = cw.visitAnnotation("Lsoftware/coley/recaf/plugin/PluginInformation;", true);
			av.visit("id", "dummy");
			av.visit("name", "dummy");
			av.visit("version", "1.0");
			av.visitEnd();
			MethodVisitor mv;
			{
				mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
				mv.visitInsn(RETURN);
				mv.visitLabel(new Label());
				mv.visitMaxs(1, 1);
				mv.visitEnd();
			}
			{
				mv = cw.visitMethod(ACC_PUBLIC, "onEnable", "()V", null, null);
				mv.visitCode();
				Label label0 = new Label();
				Label label1 = new Label();
				Label label2 = new Label();
				mv.visitTryCatchBlock(label0, label1, label2, "java/lang/Throwable");
				Label label3 = new Label();
				Label label4 = new Label();
				Label label5 = new Label();
				mv.visitTryCatchBlock(label3, label4, label5, "java/lang/Throwable");
				Label label6 = new Label();
				Label label7 = new Label();
				Label label8 = new Label();
				mv.visitTryCatchBlock(label6, label7, label8, "java/lang/Exception");
				mv.visitLabel(label6);
				mv.visitLineNumber(10, label6);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
				mv.visitLdcInsn("file.txt");
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
				mv.visitVarInsn(ASTORE, 1);
				mv.visitLabel(label0);
				mv.visitLineNumber(11, label0);
				mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
				mv.visitTypeInsn(NEW, "java/lang/String");
				mv.visitInsn(DUP);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "readAllBytes", "()[B", false);
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
				mv.visitLabel(label1);
				mv.visitLineNumber(12, label1);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitJumpInsn(IFNULL, label7);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "close", "()V", false);
				mv.visitJumpInsn(GOTO, label7);
				mv.visitLabel(label2);
				mv.visitLineNumber(10, label2);
				mv.visitVarInsn(ASTORE, 2);
				mv.visitVarInsn(ALOAD, 1);
				Label label9 = new Label();
				mv.visitJumpInsn(IFNULL, label9);
				mv.visitLabel(label3);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "close", "()V", false);
				mv.visitLabel(label4);
				mv.visitJumpInsn(GOTO, label9);
				mv.visitLabel(label5);
				mv.visitVarInsn(ASTORE, 3);
				mv.visitVarInsn(ALOAD, 2);
				mv.visitVarInsn(ALOAD, 3);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "addSuppressed", "(Ljava/lang/Throwable;)V", false);
				mv.visitLabel(label9);
				mv.visitVarInsn(ALOAD, 2);
				mv.visitInsn(ATHROW);
				mv.visitLabel(label7);
				mv.visitLineNumber(14, label7);
				Label label10 = new Label();
				mv.visitJumpInsn(GOTO, label10);
				mv.visitLabel(label8);
				mv.visitLineNumber(12, label8);
				mv.visitVarInsn(ASTORE, 1);
				Label label11 = new Label();
				mv.visitLabel(label11);
				mv.visitLineNumber(13, label11);
				mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
				mv.visitInsn(DUP);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
				mv.visitInsn(ATHROW);
				mv.visitLabel(label10);
				mv.visitLineNumber(15, label10);
				mv.visitInsn(RETURN);
				Label label12 = new Label();
				mv.visitLabel(label12);
				mv.visitLocalVariable("s", "Ljava/io/InputStream;", null, label0, label7, 1);
				mv.visitLocalVariable("e", "Ljava/lang/Exception;", null, label11, label10, 1);
				mv.visitLocalVariable("this", "Lsoftware/coley/recaf/services/plugin/DummyPluginBody;", null, label6, label12, 0);
				mv.visitMaxs(4, 4);
				mv.visitEnd();
			}
			{
				mv = cw.visitMethod(ACC_PUBLIC, "onDisable", "()V", null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				mv.visitInsn(RETURN);
				mv.visitLabel(new Label());
				mv.visitMaxs(0, 1);
				mv.visitEnd();
			}
			cw.visitEnd();
			classBytes = cw.toByteArray();
		}
		byte[] zip = ZipCreationUtils.createZip(Map.of(
				className + ".class", classBytes,
				ZipPluginLoader.SERVICE_PATH, className.getBytes(StandardCharsets.UTF_8),
				"file.txt", "Dummy plugin says 'hello world'!".getBytes(StandardCharsets.UTF_8)
		));

		try {
			// Load the plugin
			ByteSource pluginSource = ByteSources.wrap(zip);
			PluginDiscoverer discoverer = () -> List.of(() -> pluginSource);
			PluginContainer<?> container = pluginManager.loadPlugins(discoverer).iterator().next();

			// Now unload it
			pluginManager.unloaderFor("dummy").commit();
		} catch (PluginException ex) {
			fail("Failed to load plugin", ex);
		}
	}

	@SuppressWarnings("all")
	private record PluginInformationRecord(String id, String name, String version, String author, String[] dependencies,
	                                       String description) implements PluginInformation {
		@Override
		public Class<? extends Annotation> annotationType() {
			return PluginInformation.class;
		}

		@Override
		public String[] softDependencies() {
			return new String[0];
		}
	}
}
