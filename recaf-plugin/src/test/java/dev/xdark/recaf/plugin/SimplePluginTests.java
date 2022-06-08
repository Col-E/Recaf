package dev.xdark.recaf.plugin;

import dev.xdark.recaf.plugin.java.ZipPluginLoader;
import me.coley.recaf.TestUtils;
import me.coley.recaf.io.ByteSource;
import me.coley.recaf.io.ByteSources;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests demonstrating using a {@link SimplePluginManager} to load/unload a plugin.
 *
 * @author xtherk
 */
public class SimplePluginTests extends TestUtils {
	private static final Path samplePluginDir = sourcesDir.resolve("sample-plugin");
	private static final String pluginName = "SamplePlugin";

	@Test
	public void testLoadPlugin() {
		// Create sample plugin jar, write to stream
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		compressTestSamplePlugin(out);
		// Create a plugin manager and register a zip(jar) loader
		SimplePluginManager manager = new SimplePluginManager();
		manager.registerLoader(new ZipPluginLoader(Plugin.class.getClassLoader()));
		// Attempt to load from the freshly created sample plugin jar (stored in the byte array stream)
		ByteSource byteSource = ByteSources.wrap(out.toByteArray());
		PluginContainer<Plugin> pc;
		try {
			pc = manager.loadPlugin(byteSource);
		} catch (PluginLoadException ex) {
			fail(ex);
			return;
		}
		// Assert that the plugin is loaded
		PluginInformation information = pc.getInformation();
		assertEquals(pluginName, information.getName());
		assertEquals(pc, manager.getPlugin(pluginName));
		// Unload it, then assert that the plugin is no longer accessible
		manager.unloadPlugin(pc);
		assertNull(manager.getPlugin(pluginName));
		assertTrue(manager.getPlugins().isEmpty());
	}

	/**
	 * Create {@code TestSamplePlugin.jar} from {@code SamplePlugin.class} in the test resources directory.
	 */
	private void compressTestSamplePlugin(OutputStream out) {
		try (ZipOutputStream zos = new ZipOutputStream(out)) {
			// We only need care about classes.
			Collection<File> classes = FileUtils.listFiles(samplePluginDir.toFile(), new String[]{"class"}, true);
			for (File clz : classes) {
				ZipEntry ze = new ZipEntry(toEntryName(clz));
				ze.setTime(System.currentTimeMillis());
				zos.putNextEntry(ze);
				byte[] bytes = FileUtils.readFileToByteArray(clz);
				zos.write(bytes);
				zos.closeEntry();
			}
		} catch (IOException ex) {
			fail(ex);
		}
	}

	private static String toEntryName(File classFile) {
		String path = classFile.getPath().substring((samplePluginDir + File.separator).length());
		return path.replace(File.separator, "/");
	}
}
