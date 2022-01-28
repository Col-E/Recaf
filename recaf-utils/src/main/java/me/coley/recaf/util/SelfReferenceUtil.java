package me.coley.recaf.util;

import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Utility for self-referencing the Recaf application as a file.
 *
 * @author Matt
 */
public class SelfReferenceUtil {
    private static final Logger logger = Logging.get(SelfReferenceUtil.class);
    private static SelfReferenceUtil instance = null;
    private final File file;
    private final boolean isJar;

    private SelfReferenceUtil(File file) {
        this.file = file;
        this.isJar = file.getName().toLowerCase().endsWith(".jar");
    }

    /**
     * @return File reference to self.
     */
    public File getFile() {
        return file;
    }

    /**
     * @return File path to self.
     */
    public String getPath() {
        return file.getAbsolutePath();
    }

    /**
     * @return Is the current executable context a jar file.
     */
    public boolean isJar() {
        return isJar;
    }

    /**
     * @return List of language files recognized.
     */
    public List<InternalPath> getLangs() {
        return getFiles("translations/", ".lang");
    }

    /**
     * @param prefix
     *            File prefix to match.
     * @param suffix
     *            File suffix to match <i>(such as a file extension)</i>.
     * @return List of matching files.
     */
    private List<InternalPath> getFiles(String prefix, String suffix) {
        List<InternalPath> list = new ArrayList<>();
        if (isJar()) {
            // Read self as jar
            try (ZipFile file = new ZipFile(getFile())) {
                Enumeration<? extends ZipEntry> entries = file.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    // skip directories
                    if (entry.isDirectory()) continue;
                    String name = entry.getName();
                    if (prefix != null && !name.startsWith(prefix))
                        continue;
                    if (suffix != null && !name.endsWith(suffix))
                        continue;
                    list.add(InternalPath.internal(name));
                }
            } catch (Exception ex) {
                logger.error("Failed internal file (archive) lookup: {}", getFile(), ex);
            }
        } else {
            // Gradle sucks
            Path dir = this.getFile()
                    .toPath()
                    .getParent()
                    .getParent()
                    .getParent()
                    .resolve("resources")
                    .resolve("main");
            try {
                Files.walk(dir).forEach(p -> {
                    File file = dir.relativize(p).toFile();
                    String path = file.getPath().replace('\\', '/');
                    if (prefix != null && !path.startsWith(prefix))
                        return;
                    if (suffix != null && !path.endsWith(suffix))
                        return;
                    list.add(InternalPath.internal(path));
                });
            } catch(IOException ex) {
                logger.error("Failed internal file (directory) lookup: {}", getFile(), ex);
            }
        }
        return list;
    }

    public static void createInstance(Class<?> context) {
        if (instance != null) {
            return;
        }

        try {
            CodeSource codeSource = context.getProtectionDomain().getCodeSource();
            File selfFile = new File(codeSource.getLocation().toURI().getPath());
            instance = new SelfReferenceUtil(selfFile);
        } catch (URISyntaxException e) {
            logger.error("Failed to resolve self reference", e);
        }
    }

    public static SelfReferenceUtil getInstance() {
        return instance;
    }
}