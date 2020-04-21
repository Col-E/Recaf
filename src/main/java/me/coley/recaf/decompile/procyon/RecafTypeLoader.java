package me.coley.recaf.decompile.procyon;

import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import me.coley.recaf.control.Controller;
import me.coley.recaf.util.ClassUtil;

/**
 * Type loader that lookup classes from current workspace
 *
 * @author xxDark
 */
final class RecafTypeLoader implements ITypeLoader {
    private final Controller controller;

    RecafTypeLoader(Controller controller) {
        this.controller = controller;
    }

    @Override
    public boolean tryLoadType(String name, Buffer buffer) {
        byte[] code = controller.getWorkspace().getRawClass(name);
        if (controller.config().decompile().stripDebug)
            code = ClassUtil.stripDebugForDecompile(code);
        if (code == null) return false;
        buffer.position(0);
        buffer.putByteArray(code, 0, code.length);
        buffer.position(0);
        return true;
    }
}
