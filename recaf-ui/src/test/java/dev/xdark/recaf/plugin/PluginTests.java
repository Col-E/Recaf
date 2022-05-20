package dev.xdark.recaf.plugin;

import dev.xdark.recaf.plugin.java.ZipPluginLoader;
import dev.xdark.recaf.TestUtils;
import me.coley.recaf.io.ByteSources;
import me.coley.recaf.util.Directories;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author xtherk
 */
public class PluginTests extends TestUtils {

    private final Path samplePluginDir = sourcesDir.resolve("sample-plugin");

    /**
     * This method will generate a plugin example in Recaf Plugins Folder
     */
    @Test
    @Disabled
    public void testCreateSamplePluginFile() throws IOException {
        Path target = Directories.getPluginDirectory().resolve("SamplePlugin.jar");
        FileOutputStream fos = new FileOutputStream(target.toFile());
        compressTestSamplePlugin(fos);
    }

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
     * compress TestSamplePlugin.jar
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
