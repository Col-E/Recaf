package me.coley.recaf.util;

import me.coley.recaf.util.struct.VMUtil;

import java.io.FileNotFoundException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * @author xxDark
 */
public final class Natives {

    /**
     * Disallow public constructions.
     */
    private Natives() {
    }

    /**
     * Attempts to load attach library.
     *
     * @return {@link Optional} result containing {@link Throwable}
     * if any error occurred.
     */
    public static Optional<Throwable> loadAttach() {
        try {
            System.loadLibrary("attach");
            try {
                Class.forName("com.sun.tools.attach.VirtualMachine", true, null);
                return Optional.empty();
            } catch (ClassNotFoundException ignored) { }
            if (VMUtil.getVmVersion() < 9) {
                Path toolsPath = Paths.get("lib", "tools.jar");
                Path jrePath = Paths.get(System.getProperty("java.home"));
                Path maybePath = jrePath.resolve(toolsPath);
                if (Files.notExists(maybePath)) {
                    // CD .. -> CD toolsPath
                    maybePath = jrePath.getParent().resolve(toolsPath);
                }
                if (Files.notExists(maybePath)) {
                    return Optional.of(new FileNotFoundException("Could not locate tools.jar"));
                }
                ClassLoader cl = Natives.class.getClassLoader();
                if (!(cl instanceof URLClassLoader)) {
                    cl = ClassLoader.getSystemClassLoader();
                }
                VMUtil.addURL(cl, maybePath.toUri().toURL());
            }
            return Optional.empty();
        } catch (UnsatisfiedLinkError ignored) {
            // attach library not found, that's ok.
            return Optional.empty();
        } catch (Throwable t) {
            return Optional.of(t);
        }
    }
}
