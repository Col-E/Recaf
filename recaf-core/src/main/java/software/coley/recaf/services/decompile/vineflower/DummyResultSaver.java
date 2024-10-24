package software.coley.recaf.services.decompile.vineflower;

import recaf.relocation.libs.vineflower.org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.util.jar.Manifest;

/**
 * Dummy result saver to prevent Vineflower from trying to touch disk.
 *
 * @author therathatter
 */
public class DummyResultSaver implements IResultSaver {
    @Override
    public void saveFolder(String s) {
        // no-op
    }

    @Override
    public void copyFile(String s, String s1, String s2) {
        // no-op
    }

    @Override
    public void saveClassFile(String s, String s1, String s2, String s3, int[] ints) {
        // no-op
    }

    @Override
    public void createArchive(String s, String s1, Manifest manifest) {
        // no-op
    }

    @Override
    public void saveDirEntry(String s, String s1, String s2) {
        // no-op
    }

    @Override
    public void copyEntry(String s, String s1, String s2, String s3) {
        // no-op
    }

    @Override
    public void saveClassEntry(String s, String s1, String s2, String s3, String s4) {
        // no-op
    }

    @Override
    public void closeArchive(String s, String s1) {
        // no-op
    }
}
