package software.coley.recaf.services.plugin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.services.compile.CompileMap;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.services.compile.CompilerResult;
import software.coley.recaf.services.compile.JavacArguments;
import software.coley.recaf.services.compile.JavacArgumentsBuilder;
import software.coley.recaf.services.compile.JavacCompiler;
import software.coley.recaf.services.plugin.discovery.DiscoveredPluginSource;
import software.coley.recaf.services.plugin.discovery.PluginDiscoverer;
import software.coley.recaf.services.plugin.zip.ZipPluginLoader;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.util.ZipCreationUtils;
import software.coley.recaf.util.io.ByteSource;
import software.coley.recaf.util.io.ByteSources;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PluginManager}
 */
public class PluginManagerTest extends TestBase {
	private static final String ENABLE_COUNT_PREFIX = "recaf.test.plugin.enable.";
	private static final String DISABLE_COUNT_PREFIX = "recaf.test.plugin.disable.";

	private static PluginManager pluginManager;
	private static JavacCompiler javac;

	@BeforeAll
	static void setup() {
		pluginManager = recaf.get(PluginManager.class);
		javac = recaf.get(JavacCompiler.class);
	}

	@AfterEach
	void verifyCleanSlate() {
		assertEquals(0, pluginManager.getPlugins().size(), "Plugins still loaded after test case");
	}

	@Test
	void testBatchLoadedPluginCanReferenceDependencyClasses() throws IOException {
		// Setup two plugins, one of which depends on the other.
		// The dependent plugin will call a static method on a class in the dependency plugin.
		String apiId = "api-plugin";
		String implId = "impl-plugin";
		String apiPluginClass = "test/ApiPlugin";
		String apiTypeClass = "test/ApiType";
		String implPluginClass = "test/ImplPlugin";
		Map<String, String> apiSources = new HashMap<>();
		apiSources.put(apiPluginClass, pluginSource(apiPluginClass, apiId, new String[0], "", ""));
		apiSources.put(apiTypeClass, javaSource(apiTypeClass, """
				public class ApiType {
					public static String message() {
						return "dependency-ok";
					}
				}
				"""));

		CompilerResult apiCompilation = compilePluginSources(apiPluginClass, apiSources, Map.of());
		byte[] apiZip = createPluginZip(apiPluginClass, apiCompilation.getCompilations(), Map.of());
		byte[] implZip = createPluginZip(
				implPluginClass,
				Map.of(implPluginClass, pluginSource(
						implPluginClass,
						implId,
						new String[]{apiId},
						"""
								if (!java.util.Objects.equals("dependency-ok", ApiType.message())) {
									throw new IllegalStateException("Expected dependency-ok from ApiType.message()");
								}
								""",
						""
				)),
				apiCompilation.getCompilations(),
				Map.of()
		);

		try {
			// We should be able to load both plugins in the same batch.
			PluginDiscoverer discoverer = () -> List.of(
					() -> ByteSources.wrap(apiZip),
					() -> ByteSources.wrap(implZip)
			);
			Collection<PluginContainer<?>> containers = pluginManager.loadPlugins(discoverer);

			// Validate both are loaded and accessible.
			// That implies that the dependent plugin was able to see the API plugin's classes during its onEnable().
			assertEquals(2, containers.size());
			assertNotNull(pluginManager.getPlugin(apiId));
			assertNotNull(pluginManager.getPlugin(implId));

			// Unload the API plugin, which should also unload the dependent plugin.
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

		// The plugin will attempt to enumerate the resource through ClassLoader#getResources,
		// and validate that it can see the resource and that the content is as expected.
		byte[] zip = createPluginZip(pluginClass, pluginSource(
				pluginClass,
				id,
				new String[0],
				"""
						try {
							var resources = getClass().getClassLoader().getResources("META-INF/services/example.Service");
							if (!resources.hasMoreElements()) {
								throw new IllegalStateException("Expected resource to be visible through ClassLoader#getResources");
							}
							var resource = resources.nextElement();
							try (var inputStream = resource.openStream()) {
								String content = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
								if (!java.util.Objects.equals("example.Implementation", content)) {
									throw new IllegalStateException("Unexpected resource content: " + content);
								}
							}
						} catch (java.io.IOException ex) {
							throw new java.io.UncheckedIOException(ex);
						}
						""",
				""
		), Map.of(
				resourceName, resourceContent.getBytes(StandardCharsets.UTF_8)
		));

		// It should load/run without issue.
		try {
			PluginDiscoverer discoverer = () -> List.of(() -> ByteSources.wrap(zip));
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

		// The plugin will attempt to get the resource through ClassLoader#getResource,
		// and validate that the URL contains the plugin ID, which indicates that the resource is scoped to the plugin's classloader.
		byte[] zip = createPluginZip(pluginClass, pluginSource(
				pluginClass,
				id,
				new String[0],
				"""
						var resource = getClass().getClassLoader().getResource("plugin-resource.txt");
						if (resource == null) {
							throw new IllegalStateException("Expected resource URL: plugin-resource.txt");
						}
						if (!resource.toExternalForm().contains("scoped-resource-plugin")) {
							throw new IllegalStateException("Expected resource URL to contain scoped-resource-plugin, got: " + resource);
						}
						""",
				""
		), Map.of(
				resourceName, "scoped".getBytes(StandardCharsets.UTF_8)
		));

		// It should load/run without issue.
		try {
			PluginDiscoverer discoverer = () -> List.of(() -> ByteSources.wrap(zip));
			pluginManager.loadPlugins(discoverer);
			pluginManager.unloaderFor(id).commit();
		} catch (PluginException ex) {
			fail("Failed to validate plugin resource URL scope", ex);
		}
	}

	@Test
	void testFailedEnableCallsDisableDuringRollback() throws IOException {
		String id = "failing-enable-plugin";
		String pluginClass = "test/FailingEnablePlugin";

		clearCount(ENABLE_COUNT_PREFIX, id);
		clearCount(DISABLE_COUNT_PREFIX, id);

		// The plugin will throw an exception during onEnable, which should trigger a rollback that calls onDisable.
		byte[] zip = createPluginZip(pluginClass, pluginSource(
				pluginClass,
				id,
				new String[0],
				// onEnable will throw an exception, which should trigger a rollback that calls onDisable.
				"""
						String key = "recaf.test.plugin.enable.failing-enable-plugin";
						int count = Integer.parseInt(System.getProperty(key, "0"));
						System.setProperty(key, Integer.toString(count + 1));
						throw new IllegalStateException("Intentional enable failure for test plugin: failing-enable-plugin");
						""",
				// onDisable will increment a counter to verify that it was called during rollback.
				"""
						String key = "recaf.test.plugin.disable.failing-enable-plugin";
						int count = Integer.parseInt(System.getProperty(key, "0"));
						System.setProperty(key, Integer.toString(count + 1));
						"""
		), Map.of());

		// Loading the plugin should fail.
		// We should see that onEnable was called once, and onDisable was called once during rollback.
		PluginDiscoverer discoverer = () -> List.of(() -> ByteSources.wrap(zip));
		assertThrows(PluginException.class, () -> pluginManager.loadPlugins(discoverer),
				"Plugin loading should fail when onEnable throws");

		// Verify that onEnable was called once, and onDisable was called once during rollback.
		assertEquals(1, countOf(ENABLE_COUNT_PREFIX, id), "onEnable should have been called once");
		assertEquals(1, countOf(DISABLE_COUNT_PREFIX, id), "onDisable should have been called during rollback");
		assertEquals(0, pluginManager.getPlugins().size(), "Failed plugin should not remain loaded");
	}

	@Test
	void testDiamondDependencyUnloadDoesNotUnloadSamePluginTwice() throws IOException {
		String pluginA = "diamond-a";
		String pluginB = "diamond-b";
		String pluginC = "diamond-c";
		String pluginD = "diamond-d";

		String classA = "test/DiamondA";
		String classB = "test/DiamondB";
		String classC = "test/DiamondC";
		String classD = "test/DiamondD";

		clearCount(ENABLE_COUNT_PREFIX, pluginA);
		clearCount(ENABLE_COUNT_PREFIX, pluginB);
		clearCount(ENABLE_COUNT_PREFIX, pluginC);
		clearCount(ENABLE_COUNT_PREFIX, pluginD);
		clearCount(DISABLE_COUNT_PREFIX, pluginA);
		clearCount(DISABLE_COUNT_PREFIX, pluginB);
		clearCount(DISABLE_COUNT_PREFIX, pluginC);
		clearCount(DISABLE_COUNT_PREFIX, pluginD);

		byte[] zipA = createPluginZip(classA, pluginSource(
				classA, pluginA, new String[0],
				"""
						String key = "recaf.test.plugin.enable.diamond-a";
						int count = Integer.parseInt(System.getProperty(key, "0"));
						System.setProperty(key, Integer.toString(count + 1));
						""",
				"""
						String key = "recaf.test.plugin.disable.diamond-a";
						int count = Integer.parseInt(System.getProperty(key, "0"));
						System.setProperty(key, Integer.toString(count + 1));
						"""
		), Map.of());
		byte[] zipB = createPluginZip(classB, pluginSource(
				classB, pluginB, new String[]{pluginA},
				"""
						String key = "recaf.test.plugin.enable.diamond-b";
						int count = Integer.parseInt(System.getProperty(key, "0"));
						System.setProperty(key, Integer.toString(count + 1));
						""",
				"""
						String key = "recaf.test.plugin.disable.diamond-b";
						int count = Integer.parseInt(System.getProperty(key, "0"));
						System.setProperty(key, Integer.toString(count + 1));
						"""
		), Map.of());
		byte[] zipC = createPluginZip(classC, pluginSource(
				classC, pluginC, new String[]{pluginA},
				"""
						String key = "recaf.test.plugin.enable.diamond-c";
						int count = Integer.parseInt(System.getProperty(key, "0"));
						System.setProperty(key, Integer.toString(count + 1));
						""",
				"""
						String key = "recaf.test.plugin.disable.diamond-c";
						int count = Integer.parseInt(System.getProperty(key, "0"));
						System.setProperty(key, Integer.toString(count + 1));
						"""
		), Map.of());
		byte[] zipD = createPluginZip(classD, pluginSource(
				classD, pluginD, new String[]{pluginB, pluginC},
				"""
						String key = "recaf.test.plugin.enable.diamond-d";
						int count = Integer.parseInt(System.getProperty(key, "0"));
						System.setProperty(key, Integer.toString(count + 1));
						""",
				"""
						String key = "recaf.test.plugin.disable.diamond-d";
						int count = Integer.parseInt(System.getProperty(key, "0"));
						System.setProperty(key, Integer.toString(count + 1));
						"""
		), Map.of());

		try {
			// Load all plugins in a single batch, which should succeed.
			PluginDiscoverer discoverer = () -> List.of(
					() -> ByteSources.wrap(zipA),
					() -> ByteSources.wrap(zipB),
					() -> ByteSources.wrap(zipC),
					() -> ByteSources.wrap(zipD)
			);
			pluginManager.loadPlugins(discoverer);
			assertEquals(4, pluginManager.getPlugins().size());

			// Unload the root of the diamond dependency graph, which should unload all plugins.
			pluginManager.unloaderFor(pluginA).commit();
			assertEquals(0, pluginManager.getPlugins().size());

			// Verify that each plugin's onEnable and onDisable were called exactly once.
			assertEquals(1, countOf(ENABLE_COUNT_PREFIX, pluginA));
			assertEquals(1, countOf(ENABLE_COUNT_PREFIX, pluginB));
			assertEquals(1, countOf(ENABLE_COUNT_PREFIX, pluginC));
			assertEquals(1, countOf(ENABLE_COUNT_PREFIX, pluginD));
			assertEquals(1, countOf(DISABLE_COUNT_PREFIX, pluginA));
			assertEquals(1, countOf(DISABLE_COUNT_PREFIX, pluginB));
			assertEquals(1, countOf(DISABLE_COUNT_PREFIX, pluginC));
			assertEquals(1, countOf(DISABLE_COUNT_PREFIX, pluginD));
		} catch (PluginException ex) {
			fail("Failed to unload diamond dependency graph", ex);
		}
	}

	@Test
	void testCircularDependencyFailsDuringEnable() throws IOException {
		String pluginA = "circular-a";
		String pluginB = "circular-b";
		String classA = "test/CircularA";
		String classB = "test/CircularB";
		byte[] zipA = createPluginZip(classA, pluginSource(classA, pluginA, new String[]{pluginB}, "", ""), Map.of());
		byte[] zipB = createPluginZip(classB, pluginSource(classB, pluginB, new String[]{pluginA}, "", ""), Map.of());

		// If we have a circular dependency, the plugin manager should fail to load the plugins during enablement.
		PluginDiscoverer discoverer = () -> List.of(
				() -> ByteSources.wrap(zipA),
				() -> ByteSources.wrap(zipB)
		);
		assertThrows(PluginException.class, () -> pluginManager.loadPlugins(discoverer),
				"Circular plugin dependencies should fail during enablement");
		assertEquals(0, pluginManager.getPlugins().size(), "Circular dependency failure should not leave plugins loaded");
	}

	@Test
	void testSingleLoadAndUnload() throws IOException {
		String id = "test-plugin";
		String name = "test plugin";
		String version = "test-version";
		String author = "test-author";
		String description = "test-description";
		String className = "test/PluginTest";
		byte[] zip = createPluginZip(className, pluginSource(
				className, id, name, version, author, description, new String[0], "", ""
		), Map.of());

		try {
			ByteSource pluginSource = ByteSources.wrap(zip);
			PluginDiscoverer discoverer = () -> List.of(() -> pluginSource);
			PluginContainer<?> container = pluginManager.loadPlugins(discoverer).iterator().next();

			// Validate info matches expected values.
			PluginInfo information = container.info();
			assertEquals(id, information.id());
			assertEquals(name, information.name());
			assertEquals(version, information.version());
			assertEquals(author, information.author());
			assertEquals(description, information.description());

			// Validate that the plugin is loaded and accessible.
			assertEquals(1, pluginManager.getPlugins().size());
			assertSame(container, pluginManager.getPlugin(id));

			// Validate that attempting to load the same plugin again fails.
			assertThrows(PluginException.class, () -> pluginManager.loadPlugins(discoverer), "Duplicate plugin loading should fail");
			assertEquals(1, pluginManager.getPlugins().size());
			assertSame(container, pluginManager.getPlugin(id));

			// Unload the plugin and validate that it is no longer accessible.
			pluginManager.unloaderFor(id).commit();
			assertEquals(0, pluginManager.getPlugins().size());
			assertNull(pluginManager.getPlugin(id));
		} catch (PluginException ex) {
			fail("Failed to load plugin", ex);
		}
	}

	@Test
	void testDependentChain() throws IOException {
		// 1 <-- 2 <-- 3
		List<DiscoveredPluginSource> sources = new ArrayList<>(3);
		for (int i = 0; i < 3; i++) {
			String id = "test-plugin-" + i;
			String name = "test " + i;
			String version = "test-version";
			String author = "test-author";
			String description = "test-description";
			String className = "test/PluginTest" + i;
			String[] dependencies = i > 0 ? new String[]{"test-plugin-" + (i - 1)} : new String[0];
			byte[] zip = createPluginZip(className, pluginSource(
					className, id, name, version, author, description, dependencies, "", ""
			), Map.of());
			sources.add(() -> ByteSources.wrap(zip));
		}

		try {
			// Load all plugins in a single batch, which should succeed.
			PluginDiscoverer discoverer = () -> sources;
			pluginManager.loadPlugins(discoverer);
			assertEquals(3, pluginManager.getPlugins().size());

			// Unload the root of the dependency chain, which should unload all plugins.
			pluginManager.unloaderFor("test-plugin-0").commit();
			assertEquals(0, pluginManager.getPlugins().size());
			for (int i = 0; i < 3; i++) {
				String id = "test-plugin-" + i;
				assertNull(pluginManager.getPlugin(id));
			}
		} catch (PluginException ex) {
			fail("Failed to load plugins", ex);
		}
	}

	private static void clearCount(String prefix, String id) {
		System.clearProperty(prefix + id);
	}

	private static int countOf(String prefix, String id) {
		return Integer.parseInt(System.getProperty(prefix + id, "0"));
	}

	private static byte[] createPluginZip(String pluginInternalName, String pluginSource, Map<String, byte[]> additionalEntries) throws IOException {
		return createPluginZip(pluginInternalName, Map.of(pluginInternalName, pluginSource), Map.of(), additionalEntries);
	}

	private static byte[] createPluginZip(String pluginInternalName, Map<String, String> classSources, Map<String, byte[]> additionalEntries) throws IOException {
		return createPluginZip(pluginInternalName, classSources, Map.of(), additionalEntries);
	}

	private static byte[] createPluginZip(String pluginInternalName, Map<String, String> classSources,
	                                      Map<String, byte[]> compileClasspathClasses, Map<String, byte[]> additionalEntries) throws IOException {
		return createPluginZip(pluginInternalName, compilePluginSources(pluginInternalName, classSources, compileClasspathClasses).getCompilations(), additionalEntries);
	}

	private static byte[] createPluginZip(String pluginInternalName, CompileMap compilations, Map<String, byte[]> additionalEntries) throws IOException {
		Map<String, byte[]> entries = new HashMap<>();
		compilations.forEach((name, bytes) -> entries.put(name + ".class", bytes));
		entries.put(ZipPluginLoader.SERVICE_PATH, pluginInternalName.replace('/', '.').getBytes(StandardCharsets.UTF_8));
		entries.putAll(additionalEntries);
		return ZipCreationUtils.createZip(entries);
	}

	private static CompilerResult compilePluginSources(String pluginInternalName, Map<String, String> classSources,
	                                                   Map<String, byte[]> compileClasspathClasses) {
		Workspace workspace = compileClasspathClasses.isEmpty() ? null : workspaceFromCompilations(compileClasspathClasses);
		JavacArguments arguments = new JavacArgumentsBuilder()
				.withClassName(pluginInternalName)
				.withClassSources(classSources)
				.build();
		CompilerResult result = javac.compile(arguments, workspace, null);
		if (!result.wasSuccess())
			throw new AssertionError("Failed to compile plugin sources for '" + pluginInternalName + "':\n" + formatCompilationFailure(result));
		return result;
	}

	private static Workspace workspaceFromCompilations(Map<String, byte[]> compileClasspathClasses) {
		BasicJvmClassBundle bundle = new BasicJvmClassBundle();
		compileClasspathClasses.values().forEach(bytes -> bundle.initialPut(new JvmClassInfoBuilder(bytes).build()));
		return TestClassUtils.fromBundle(bundle);
	}

	private static String formatCompilationFailure(CompilerResult result) {
		StringBuilder sb = new StringBuilder();
		if (result.getException() != null)
			sb.append(result.getException()).append('\n');
		if (!result.getDiagnostics().isEmpty()) {
			String diagnostics = result.getDiagnostics().stream()
					.map(CompilerDiagnostic::toString)
					.collect(Collectors.joining("\n"));
			sb.append(diagnostics).append('\n');
		}
		if (sb.isEmpty())
			sb.append("Unknown compilation failure");
		return sb.toString().stripTrailing();
	}

	private static String pluginSource(String internalName, String id, String[] dependencies, String onEnableBody, String onDisableBody) {
		return pluginSource(internalName, id, id, "1.0", "", "", dependencies, onEnableBody, onDisableBody);
	}

	private static String pluginSource(String internalName, String id, String name, String version, String author,
	                                   String description, String[] dependencies, String onEnableBody, String onDisableBody) {
		String simpleName = simpleName(internalName);
		String annotation = pluginInformationAnnotation(id, name, version, author, description, dependencies);
		String enableBody = methodBody(onEnableBody);
		String disableBody = methodBody(onDisableBody);
		return javaSource(internalName, """
				import software.coley.recaf.plugin.Plugin;
				import software.coley.recaf.plugin.PluginInformation;
				
				%s
				public class %s implements Plugin {
					@Override
					public void onEnable() {
				%s
					}
				
					@Override
					public void onDisable() {
				%s
					}
				}
				""".formatted(annotation, simpleName, enableBody, disableBody));
	}

	private static String javaSource(String internalName, String body) {
		String packageDeclaration = packageDeclaration(internalName);
		String sourceBody = body.stripIndent().strip();
		if (packageDeclaration.isEmpty())
			return sourceBody + '\n';
		return packageDeclaration + "\n" + sourceBody + '\n';
	}

	private static String packageDeclaration(String internalName) {
		int separatorIndex = internalName.lastIndexOf('/');
		if (separatorIndex < 0)
			return "";
		return "package " + internalName.substring(0, separatorIndex).replace('/', '.') + ";";
	}

	private static String simpleName(String internalName) {
		int separatorIndex = internalName.lastIndexOf('/');
		return separatorIndex < 0 ? internalName : internalName.substring(separatorIndex + 1);
	}

	private static String methodBody(String body) {
		String stripped = body.stripIndent().strip();
		if (stripped.isEmpty())
			return "";
		return indent(stripped, 8);
	}

	private static String indent(String text, int spaces) {
		String padding = " ".repeat(spaces);
		return text.lines()
				.map(line -> padding + line)
				.collect(Collectors.joining("\n"));
	}

	private static String pluginInformationAnnotation(String id, String name, String version, String author,
	                                                  String description, String[] dependencies) {
		StringJoiner joiner = new StringJoiner(", ", "@PluginInformation(", ")");
		joiner.add("id = " + stringLiteral(id));
		joiner.add("name = " + stringLiteral(name));
		joiner.add("version = " + stringLiteral(version));
		if (!author.isEmpty())
			joiner.add("author = " + stringLiteral(author));
		if (!description.isEmpty())
			joiner.add("description = " + stringLiteral(description));
		if (dependencies.length > 0)
			joiner.add("dependencies = " + stringArrayLiteral(dependencies));
		return joiner.toString();
	}

	private static String stringArrayLiteral(String[] values) {
		return values.length == 0 ? "{}" : "{ " + List.of(values).stream()
				.map(PluginManagerTest::stringLiteral)
				.collect(Collectors.joining(", ")) + " }";
	}

	private static String stringLiteral(String value) {
		return "\"" + value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\r", "\\r")
				.replace("\n", "\\n") + "\"";
	}
}
