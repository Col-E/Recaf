package software.coley.recaf.services.decompile.fernflower;

import java.io.File;

/**
 * FakeFile
 *
 * @author meiMingle
 */
public class FakeFile extends File {

    String absolutePath;


    public FakeFile(String path) {
        super(path);
        this.absolutePath = path;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public String getAbsolutePath() {
        return absolutePath;
    }
}
