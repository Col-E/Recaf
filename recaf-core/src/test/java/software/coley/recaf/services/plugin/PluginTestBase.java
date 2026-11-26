package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import software.coley.recaf.util.io.ByteSources;
import software.coley.recaf.util.io.ZipCreationUtils;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for tests that load plugins through {@link PluginManager}.
 * <p>
 * Plugins are generated as Java source, compiled, and packaged as archives. Plugin code reports what it observed by
 * writing system properties under {@code recaf.test.plugin.}, which are cleared after every test.
 * <p>
 * Tests must unload the plugins they load. After each test, any plugin still loaded is unloaded and reported as a
 * failure, and any CDI container still running is reported as a failure. This keeps one failing test from breaking
 * the tests that follow it.
 */
public abstract class PluginTestBase extends TestBase {
	private static final String PROPERTY_PREFIX = "recaf.test.plugin.";
	protected static PluginManager pluginManager;
	private static JavacCompiler javac;
	private Set<String> weldContainersAtStart;

	@BeforeAll
	static void setupServices() {
		pluginManager = recaf.get(PluginManager.class);
		javac = recaf.get(JavacCompiler.class);
	}

	@BeforeEach
	void recordWeldContainers() {
		weldContainersAtStart = runningWeldContainerIds();
	}

	@AfterEach
	void ensureCleanSlate() throws PluginException {
		List<String> leftover = pluginManager.getPlugins().stream()
				.map(container -> container.info().id())
				.toList();
		try {
			for (String id : leftover) {
				// Unloading a dependency also unloads its dependants, so skip any that are already gone.
				if (pluginManager.isPluginLoaded(id))
					pluginManager.unloaderFor(id).commit();
			}
		} finally {
			clearPluginProperties();
		}
		assertEquals(List.of(), leftover, "Test left plugins loaded");
		assertEquals(weldContainersAtStart, runningWeldContainerIds(), "Test left CDI containers running");
	}

	/**
	 * @param name
	 * 		Name of a value that plugin code reports.
	 *
	 * @return System property key the value is reported under.
	 */
	@Nonnull
	protected static String property(@Nonnull String name) {
		return PROPERTY_PREFIX + name;
	}

	/**
	 * @param key
	 * 		System property key of an integer counter.
	 *
	 * @return Plugin code that increments the counter.
	 */
	@Nonnull
	protected static String counterIncrement(@Nonnull String key) {
		return "System.setProperty(\"%1$s\", Integer.toString(Integer.parseInt(System.getProperty(\"%1$s\", \"0\")) + 1));"
				.formatted(key);
	}

	/**
	 * @param key
	 * 		System property key of an integer counter.
	 *
	 * @return Current value of the counter, or {@code 0} if it was never incremented.
	 */
	protected static int counterValue(@Nonnull String key) {
		return Integer.parseInt(System.getProperty(key, "0"));
	}

	/**
	 * @param internalName
	 * 		Internal name of a class, such as {@code test/Foo}.
	 *
	 * @return Simple name of the class, such as {@code Foo}.
	 */
	@Nonnull
	protected static String simpleName(@Nonnull String internalName) {
		return internalName.substring(internalName.lastIndexOf('/') + 1);
	}

	/**
	 * @param id
	 * 		ID of the plugin.
	 * @param dependencies
	 * 		IDs of the plugin's dependencies.
	 *
	 * @return Plugin information with placeholder name and version.
	 */
	@Nonnull
	protected static PluginInfo pluginInfo(@Nonnull String id, @Nonnull String... dependencies) {
		return new PluginInfo(id, id, "1.0", "", "", Set.of(dependencies), Set.of());
	}

	/**
	 * @param internalName
	 * 		Internal name of the plugin class.
	 * @param info
	 * 		Plugin information for the {@code @PluginInformation} annotation.
	 * @param onEnable
	 * 		Statements for the body of {@code onEnable}.
	 * @param onDisable
	 * 		Statements for the body of {@code onDisable}.
	 *
	 * @return Source of the plugin class.
	 */
	@Nonnull
	protected static String pluginSource(@Nonnull String internalName, @Nonnull PluginInfo info,
	                                     @Nonnull String onEnable, @Nonnull String onDisable) {
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
				""".formatted(informationAnnotation(info), simpleName(internalName), onEnable, onDisable));
	}

	/**
	 * @param internalName
	 * 		Internal name of the class.
	 * @param body
	 * 		Source after the package declaration.
	 *
	 * @return Java source with a package declaration matching the internal name.
	 */
	@Nonnull
	protected static String javaSource(@Nonnull String internalName, @Nonnull String body) {
		int slash = internalName.lastIndexOf('/');
		if (slash < 0)
			return body;
		return "package " + internalName.substring(0, slash).replace('/', '.') + ";\n" + body;
	}

	/**
	 * @param info
	 * 		Plugin information.
	 *
	 * @return {@code @PluginInformation} annotation source with the given values.
	 */
	@Nonnull
	protected static String informationAnnotation(@Nonnull PluginInfo info) {
		StringJoiner annotation = new StringJoiner(", ", "@PluginInformation(", ")");
		annotation.add("id = " + literal(info.id()));
		annotation.add("name = " + literal(info.name()));
		annotation.add("version = " + literal(info.version()));
		if (!info.author().isEmpty())
			annotation.add("author = " + literal(info.author()));
		if (!info.description().isEmpty())
			annotation.add("description = " + literal(info.description()));
		if (!info.dependencies().isEmpty())
			annotation.add("dependencies = " + literalArray(info.dependencies()));
		if (!info.softDependencies().isEmpty())
			annotation.add("softDependencies = " + literalArray(info.softDependencies()));
		return annotation.toString();
	}

	/**
	 * @param entryClass
	 * 		Internal name of the plugin entry point, used for diagnostics.
	 * @param sources
	 * 		Internal class name to Java source.
	 * @param classpath
	 * 		Compiled classes visible to the sources. May be empty.
	 *
	 * @return Compiled classes.
	 */
	@Nonnull
	protected static CompileMap compile(@Nonnull String entryClass,
	                                    @Nonnull Map<String, String> sources,
	                                    @Nonnull Map<String, byte[]> classpath) {
		JavacArguments arguments = new JavacArgumentsBuilder()
				.withClassName(entryClass)
				.withClassSources(sources)
				.build();
		CompilerResult result = javac.compile(arguments, classpathWorkspace(classpath), null);
		if (!result.wasSuccess())
			throw new AssertionError("Failed to compile plugin sources for '" + entryClass + "':\n" + describeFailure(result));
		return result.getCompilations();
	}

	/**
	 * @param classpath
	 * 		Compiled classes to expose to a compilation.
	 *
	 * @return Workspace containing the classpath classes, or {@code null} if there are none.
	 */
	@Nullable
	private static Workspace classpathWorkspace(@Nonnull Map<String, byte[]> classpath) {
		if (classpath.isEmpty())
			return null;
		BasicJvmClassBundle bundle = new BasicJvmClassBundle();
		classpath.values().forEach(bytes -> bundle.initialPut(new JvmClassInfoBuilder(bytes).build()));
		return TestClassUtils.fromBundle(bundle);
	}

	/**
	 * @param entryClass
	 * 		Internal name of the plugin entry point. Written to the service entry.
	 * @param classes
	 * 		Compiled classes, keyed by internal name.
	 * @param extraEntries
	 * 		Additional archive entries, keyed by path.
	 *
	 * @return Plugin archive bytes.
	 */
	@Nonnull
	protected static byte[] archive(@Nonnull String entryClass,
	                                @Nonnull Map<String, byte[]> classes,
	                                @Nonnull Map<String, byte[]> extraEntries) throws IOException {
		Map<String, byte[]> entries = new HashMap<>();
		classes.forEach((name, bytes) -> entries.put(name + ".class", bytes));
		entries.put(ZipPluginLoader.SERVICE_PATH, entryClass.replace('/', '.').getBytes(StandardCharsets.UTF_8));
		entries.putAll(extraEntries);
		return ZipCreationUtils.createZip(entries);
	}

	/**
	 * @param entryClass
	 * 		Internal name of the plugin entry point, which is also the only source.
	 * @param source
	 * 		Source of the plugin entry point.
	 *
	 * @return Plugin archive bytes.
	 */
	@Nonnull
	protected static byte[] pluginArchive(@Nonnull String entryClass, @Nonnull String source) throws IOException {
		return pluginArchive(entryClass, Map.of(entryClass, source), Map.of());
	}

	/**
	 * @param entryClass
	 * 		Internal name of the plugin entry point.
	 * @param sources
	 * 		Internal class name to Java source.
	 * @param extraEntries
	 * 		Additional archive entries, keyed by path.
	 *
	 * @return Plugin archive bytes.
	 */
	@Nonnull
	protected static byte[] pluginArchive(@Nonnull String entryClass,
	                                      @Nonnull Map<String, String> sources,
	                                      @Nonnull Map<String, byte[]> extraEntries) throws IOException {
		return archive(entryClass, compile(entryClass, sources, Map.of()), extraEntries);
	}

	/**
	 * @param archives
	 * 		Plugin archives to discover, one source each.
	 *
	 * @return Discoverer yielding the archives.
	 */
	@Nonnull
	protected static PluginDiscoverer discoverArchives(@Nonnull byte[]... archives) {
		List<DiscoveredPluginSource> sources = new ArrayList<>(archives.length);
		for (byte[] archive : archives)
			sources.add(() -> ByteSources.wrap(archive));
		return () -> sources;
	}

	@Nonnull
	private static String literalArray(@Nonnull Set<String> values) {
		return values.stream().sorted().map(PluginTestBase::literal).collect(Collectors.joining(", ", "{ ", " }"));
	}

	@Nonnull
	private static String literal(@Nonnull String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	@Nonnull
	private static String describeFailure(@Nonnull CompilerResult result) {
		List<String> lines = new ArrayList<>();
		if (result.getException() != null)
			lines.add(result.getException().toString());
		for (CompilerDiagnostic diagnostic : result.getDiagnostics())
			lines.add(diagnostic.toString());
		return lines.isEmpty() ? "Unknown compilation failure" : String.join("\n", lines);
	}

	@Nonnull
	private static Set<String> runningWeldContainerIds() {
		return Set.copyOf(WeldContainer.getRunningContainerIds());
	}

	private static void clearPluginProperties() {
		for (String name : System.getProperties().stringPropertyNames()) {
			if (name.startsWith(PROPERTY_PREFIX))
				System.clearProperty(name);
		}
	}
}
