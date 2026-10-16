package software.coley.recaf.ui.pane;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.coley.recaf.plugin.Plugin;
import software.coley.recaf.plugin.PluginInformation;
import software.coley.recaf.services.ServiceConfig;
import software.coley.recaf.services.file.RecafDirectoriesConfig;
import software.coley.recaf.services.plugin.ClassAllocator;
import software.coley.recaf.services.plugin.PluginContainer;
import software.coley.recaf.services.plugin.PluginException;
import software.coley.recaf.services.plugin.PluginInfo;
import software.coley.recaf.services.plugin.PluginLoader;
import software.coley.recaf.services.plugin.PluginManager;
import software.coley.recaf.services.plugin.PluginUnloader;
import software.coley.recaf.services.plugin.discovery.DiscoveredPluginSource;
import software.coley.recaf.services.plugin.discovery.PluginDiscoverer;
import software.coley.recaf.services.plugin.zip.ZipPluginLoader;
import software.coley.recaf.ui.BaseFxTest;
import software.coley.recaf.ui.pane.PluginManagerPane.LocalPluginFile;
import software.coley.recaf.ui.pane.sample.SampleAlphaPlugin;
import software.coley.recaf.ui.pane.sample.SampleBetaPlugin;
import software.coley.recaf.util.io.ByteSource;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PluginManagerPane} plugin file management: scanning, enable/disable, install, uninstall.
 * <p/>
 * These tests exercise only the synchronous business logic of the pane,
 * bypassing JavaFX UI initialization via the package-private testing constructor.
 *
 * @author Canrad
 */
class PluginManagerPaneTest extends BaseFxTest {
	private Path home;
	private Path pluginDir;
	private Path disabledDir;
	private Path stagingDir;
	private FakePluginManager manager;
	private PluginManagerPane pane;

	@BeforeEach
	void setup() throws Exception {
		// Fresh directories per test. Windows keeps memory-mapped plugin jars locked until GC,
		// so isolating each test's files avoids cross-test delete/move conflicts.
		home = Files.createTempDirectory("recaf-plugin-pane-test");
		pluginDir = Files.createDirectories(home.resolve("plugins"));
		disabledDir = Files.createDirectories(pluginDir.resolve("disabled"));
		stagingDir = Files.createDirectories(home.resolve("staging"));

		RecafDirectoriesConfig dirs = mock(RecafDirectoriesConfig.class);
		when(dirs.getPluginDirectory()).thenReturn(pluginDir);
		when(dirs.getDisabledPluginDirectory()).thenReturn(disabledDir);

		manager = new FakePluginManager();
		pane = new PluginManagerPane(manager, dirs, true);
	}

	@AfterEach
	void cleanup() throws Exception {
		if (home != null) {
			try (var walk = Files.walk(home)) {
				walk.sorted(java.util.Comparator.reverseOrder())
						.forEach(p -> {
							try {
								Files.deleteIfExists(p);
							} catch (IOException ignored) {}
						});
			}
		}
	}

	@Test
	void testScanFindsEnabledAndDisabled() throws Exception {
		writePluginJar(pluginDir.resolve("alpha.jar"), SampleAlphaPlugin.class);
		writePluginJar(disabledDir.resolve("beta.jar"), SampleBetaPlugin.class);

		List<LocalPluginFile> files = pane.scanPluginFiles();
		assertEquals(2, files.size(), "Should find both the enabled and disabled plugin");

		LocalPluginFile alpha = find(files, "sample-alpha");
		LocalPluginFile beta = find(files, "sample-beta");
		assertTrue(alpha.enabled(), "Plugin in the plugin directory should be enabled");
		assertFalse(beta.enabled(), "Plugin in the disabled directory should be disabled");
		assertEquals("Sample Alpha", alpha.info().name());
		assertEquals("Sample Beta", beta.info().name());
	}

	@Test
	void testDisableMovesToDisabledAndUnloads() throws Exception {
		Path jar = pluginDir.resolve("alpha.jar");
		writePluginJar(jar, SampleAlphaPlugin.class);
		manager.loaded.add("sample-alpha");
		LocalPluginFile alpha = entry(jar, SampleAlphaPlugin.class, true);

		pane.applyEnabled(alpha, false);

		assertFalse(Files.exists(jar), "Jar should no longer be in the enabled directory");
		assertTrue(Files.exists(disabledDir.resolve("alpha.jar")), "Jar should be moved to the disabled directory");
		assertFalse(manager.isPluginLoaded("sample-alpha"), "Plugin should be unloaded after disabling");
	}

	@Test
	void testEnableMovesBackAndLoads() throws Exception {
		Path disabledJar = disabledDir.resolve("alpha.jar");
		writePluginJar(disabledJar, SampleAlphaPlugin.class);
		LocalPluginFile alpha = entry(disabledJar, SampleAlphaPlugin.class, false);

		pane.applyEnabled(alpha, true);

		assertFalse(Files.exists(disabledJar), "Jar should no longer be in the disabled directory");
		assertTrue(Files.exists(pluginDir.resolve("alpha.jar")), "Jar should be moved to the enabled directory");
		assertTrue(manager.isPluginLoaded("sample-alpha"), "Plugin should be loaded after enabling");
	}

	@Test
	void testInstallCopiesAndLoads() throws Exception {
		Path source = stagingDir.resolve("alpha.jar");
		writePluginJar(source, SampleAlphaPlugin.class);

		pane.installFrom(source);

		assertTrue(Files.exists(source), "Source jar should be left untouched");
		assertTrue(Files.exists(pluginDir.resolve("alpha.jar")), "Jar should be copied into the plugin directory");
		assertTrue(manager.isPluginLoaded("sample-alpha"), "Plugin should be loaded after install");
	}

	@Test
	void testUninstallDeletesAndUnloads() throws Exception {
		Path jar = pluginDir.resolve("alpha.jar");
		writePluginJar(jar, SampleAlphaPlugin.class);
		manager.loaded.add("sample-alpha");
		LocalPluginFile alpha = entry(jar, SampleAlphaPlugin.class, true);

		pane.applyUninstall(alpha);

		assertFalse(Files.exists(jar), "Jar should be deleted after uninstall");
		assertFalse(manager.isPluginLoaded("sample-alpha"), "Plugin should be unloaded after uninstall");
	}

	@Nonnull
	private static LocalPluginFile find(@Nonnull List<LocalPluginFile> files, @Nonnull String id) {
		return files.stream().filter(f -> f.info().id().equals(id)).findFirst()
				.orElseThrow(() -> new AssertionError("No plugin file with id: " + id));
	}

	/**
	 * Builds a {@link LocalPluginFile} directly from the plugin's annotation, without memory-mapping the jar.
	 * This avoids leaving a Windows file lock on a jar that the test is about to move or delete.
	 */
	@Nonnull
	private static LocalPluginFile entry(@Nonnull Path path, @Nonnull Class<? extends Plugin> cls, boolean enabled) {
		PluginInformation a = cls.getAnnotation(PluginInformation.class);
		PluginInfo info = new PluginInfo(a.id(), a.name(), a.version(), a.author(), a.description(),
				Set.of(a.dependencies()), Set.of(a.softDependencies()));
		return new LocalPluginFile(path, info, enabled);
	}

	/**
	 * Packages a compiled plugin class into a valid plugin jar (class bytes + service descriptor).
	 */
	private static void writePluginJar(@Nonnull Path jarPath, @Nonnull Class<? extends Plugin> pluginClass) throws IOException {
		String classResource = pluginClass.getName().replace('.', '/') + ".class";
		byte[] classBytes;
		try (var in = PluginManagerPaneTest.class.getClassLoader().getResourceAsStream(classResource)) {
			assertNotNull(in, "Missing compiled class: " + classResource);
			classBytes = in.readAllBytes();
		}
		try (OutputStream fos = Files.newOutputStream(jarPath);
		     ZipOutputStream zos = new ZipOutputStream(fos)) {
			zos.putNextEntry(new ZipEntry(classResource));
			zos.write(classBytes);
			zos.closeEntry();
			zos.putNextEntry(new ZipEntry("META-INF/services/" + Plugin.class.getName()));
			zos.write(pluginClass.getName().getBytes());
			zos.closeEntry();
		}
	}

	/**
	 * Functional {@link PluginManager} that tracks loaded plugin ids by parsing jars with {@link ZipPluginLoader},
	 * so {@link #isPluginLoaded} reflects the real load/unload effects triggered by the pane.
	 */
	private static class FakePluginManager implements PluginManager {
		private final Set<String> loaded = new HashSet<>();
		private final ZipPluginLoader loader = new ZipPluginLoader();

		@Nonnull
		@Override
		public Collection<PluginContainer<?>> loadPlugins(@Nonnull PluginDiscoverer discoverer) throws PluginException {
			for (DiscoveredPluginSource source : discoverer.findSources()) {
				ByteSource bytes = source.source();
				var prepared = loader.prepare(bytes);
				if (prepared != null) {
					loaded.add(prepared.info().id());
					prepared.reject();
				}
			}
			return List.of();
		}

		@Nonnull
		@Override
		public PluginUnloader unloaderFor(@Nonnull String id) {
			return new PluginUnloader() {
				@Override
				public void commit() {
					loaded.remove(id);
				}

				@Nonnull
				@Override
				public PluginInfo unloadingPlugin() {
					return PluginInfo.empty().withId(id);
				}

				@Nonnull
				@Override
				public Stream<PluginInfo> dependants() {
					return Stream.empty();
				}
			};
		}

		@Override
		public boolean isPluginLoaded(@Nonnull String id) {
			return loaded.contains(id);
		}

		@Nonnull
		@Override
		public ClassAllocator getAllocator() {
			throw new UnsupportedOperationException();
		}

		@Nullable
		@Override
		public <T extends Plugin> PluginContainer<T> getPlugin(@Nonnull String id) {
			return null;
		}

		@Nonnull
		@Override
		public Collection<PluginContainer<?>> getPlugins() {
			return List.of();
		}

		@Override
		public void registerLoader(@Nonnull PluginLoader loader) {}

		@Nonnull
		@Override
		public String getServiceId() {
			return "fake-plugin-manager";
		}

		@Nonnull
		@Override
		public ServiceConfig getServiceConfig() {
			return mock(ServiceConfig.class);
		}
	}
}
