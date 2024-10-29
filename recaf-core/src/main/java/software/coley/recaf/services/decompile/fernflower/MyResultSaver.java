package software.coley.recaf.services.decompile.fernflower;


import recaf.relocation.libs.fernflower.org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.util.jar.Manifest;

/**
 * MyResultSaver
 *
 * @author meiMingle
 */
public class MyResultSaver implements IResultSaver {
    private String result = "";
    private int[] mapping;

    public final String getResult() {
        return this.result;
    }

    public final void setResult(String string) {
        this.result = string;
    }

    public final int[] getMapping() {
        return this.mapping;
    }

    public final void setMapping(int[] nArray) {
        this.mapping = nArray;
    }

    @Override
    public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
        if (((CharSequence) this.result).length() == 0) {
            this.result = content;
            this.mapping = mapping;
        }
    }

    @Override
    public void saveFolder(String path) {
        // no-op
    }

    @Override
    public void copyFile(String source, String path, String entryName) {
        // no-op
    }

    @Override
    public void createArchive(String path, String archiveName, Manifest manifest) {
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