package software.coley.recaf.services.decompile.vineflower;

import org.jetbrains.java.decompiler.main.extern.IContextSource;
import software.coley.recaf.info.JvmClassInfo;

import java.io.IOException;

/**
 * Output sink for Vineflower decompiler
 *
 * @author therathatter
 */
public class DecompiledOutputSink implements IContextSource.IOutputSink {
    protected final JvmClassInfo target;
    protected final ThreadLocal<String> out = new ThreadLocal<>();

    DecompiledOutputSink(JvmClassInfo target) {
        this.target = target;
    }

    ThreadLocal<String> getDecompiledOutput() {
        return out;
    }

    @Override
    public void begin() {

    }

    @Override
    public void acceptClass(String qualifiedName, String fileName, String content, int[] mapping) {
        if (target.getName().equals(qualifiedName))
            out.set(content);
    }

    @Override
    public void acceptDirectory(String directory) {

    }

    @Override
    public void acceptOther(String path) {

    }

    @Override
    public void close() throws IOException {

    }
}
