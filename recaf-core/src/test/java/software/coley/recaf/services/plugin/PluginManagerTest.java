package software.coley.recaf.services.plugin;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.TestBase;
import software.coley.recaf.plugin.*;
import software.coley.recaf.util.ZipCreationUtils;
import software.coley.recaf.util.io.ByteSources;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;

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

	@Test
	void testLoadAndUnload() throws IOException {
		String name = "test-plugin";
		String version = "test-version";
		String author = "test-author";
		String description = "test-description";
		DynamicType.Unloaded<Plugin> unloaded = new ByteBuddy()
				.subclass(Plugin.class)
				.name("test.PluginTest")
				.defineMethod("onEnable", void.class, Modifier.PUBLIC)
				.intercept(FixedValue.originType())
				.defineMethod("onDisable", void.class, Modifier.PUBLIC)
				.intercept(FixedValue.originType())
				.annotateType(new PluginInformation() {
					@Override
					public Class<? extends Annotation> annotationType() {
						return PluginInformation.class;
					}

					@Override
					public String name() {
						return name;
					}

					@Override
					public String version() {
						return version;
					}

					@Override
					public String author() {
						return author;
					}

					@Override
					public String description() {
						return description;
					}
				}).make();
		byte[] zip = ZipCreationUtils.createSingleEntryZip("test/PluginTest.class", unloaded.getBytes());

		try {
			// Load the plugin
			PluginContainer<Plugin> container = pluginManager.loadPlugin(ByteSources.wrap(zip));

			// Assert the information stuck
			PluginInfo information = container.getInformation();
			assertEquals(name, information.getName());
			assertEquals(version, information.getVersion());
			assertEquals(author, information.getAuthor());
			assertEquals(description, information.getDescription());

			// Assert the plugin is active
			assertEquals(1, pluginManager.getPlugins().size());
			assertSame(container, pluginManager.getPlugin(name));

			// Now unload it
			pluginManager.unloadPlugin(container);

			// Assert the plugin is no longer active
			assertEquals(0, pluginManager.getPlugins().size());
			assertNull(pluginManager.getPlugin(name));
		} catch (PluginLoadException ex) {
			fail("Failed to load plugin", ex);
		}
	}
}
