package dev.xdark.recaf.plugin;

import dev.xdark.recaf.TestUtils;
import dev.xdark.recaf.plugin.java.ZipPluginLoader;
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

/**
 * @author xtherk
 */
public class SimplePluginTests extends TestUtils {

    private final Path samplePluginDir = sourcesDir.resolve("sample-plugin");


    @Test
    public void testLoadPlugin() throws PluginLoadException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        compressTestSamplePlugin(out);
        SimplePluginManager manager = new SimplePluginManager();
        manager.registerLoader(new ZipPluginLoader(Plugin.class.getClassLoader()));
        PluginContainer<Plugin> pc = manager.loadPlugin(ByteSources.wrap(out.toByteArray()));
        pc.getLoader().enablePlugin(pc);
        pc.getLoader().disablePlugin(pc);
    }

    /**
     * compress TestSamplePlugin.zip
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String toEntryName(File classFile) {
        String path = classFile.getPath().substring((samplePluginDir + File.separator).length());
        return path.replace(File.separator, "/");
    }
}
