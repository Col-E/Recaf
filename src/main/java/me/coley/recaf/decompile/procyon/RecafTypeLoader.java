package me.coley.recaf.decompile.procyon;

import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import me.coley.recaf.workspace.Workspace;

/**
 * Type loader that lookup classes from current workspace
 *
 * @author xxDark
 */
final class RecafTypeLoader implements ITypeLoader {
    private final Workspace workspace;

    RecafTypeLoader(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public boolean tryLoadType(String name, Buffer buffer) {
        byte[] bytes = workspace.getRawClass(name);
        if (bytes == null) return false;
        buffer.position(0);
        buffer.putByteArray(bytes, 0, bytes.length);
        buffer.position(0);
        return true;
    }
}
