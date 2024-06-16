package software.coley.recaf.services.plugin;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.plugin.*;
import software.coley.recaf.services.plugin.discovery.DiscoveredPluginSource;
import software.coley.recaf.services.plugin.discovery.PluginDiscoverer;
import software.coley.recaf.services.plugin.zip.ZipPluginLoader;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.util.ZipCreationUtils;
import software.coley.recaf.util.io.ByteSource;
import software.coley.recaf.util.io.ByteSources;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
