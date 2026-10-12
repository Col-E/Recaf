package software.coley.recaf.services.plugin.zip;

import org.junit.jupiter.api.Test;
import software.coley.recaf.services.plugin.PreparedPlugin;
import software.coley.recaf.services.plugin.PluginTestBase;
import software.coley.recaf.util.io.ByteSources;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ZipPluginLoader}.
 */
public class ZipPluginLoaderTest extends PluginTestBase {
	@Test
	void testZipSourceExposesSortedClassNames() throws Exception {
		String pluginClass = "test/SourceNamesPlugin";
		byte[] zip = pluginArchive(pluginClass,
				Map.of(pluginClass, pluginSource(pluginClass, pluginInfo("source-names"), "", "")),
				Map.of(
						"z/Helper.class", new byte[0],
						"a/Alpha.class", new byte[0],
						// Module and package descriptors, and multi-release entries, are not plugin classes.
						"module-info.class", new byte[0],
						"test/package-info.class", new byte[0],
						"META-INF/versions/9/legacy/Version.class", new byte[0]
				));

		PreparedPlugin prepared = new ZipPluginLoader().prepare(ByteSources.wrap(zip));
		assertNotNull(prepared);
		try {
			assertEquals(List.of("a.Alpha", "test.SourceNamesPlugin", "z.Helper"),
					prepared.pluginSource().classNames().stream().toList());
		} finally {
			prepared.reject();
		}
	}
}
