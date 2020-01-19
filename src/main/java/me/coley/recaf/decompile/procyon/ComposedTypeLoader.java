package me.coley.recaf.decompile.procyon;

import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;

import java.util.Collection;

/**
 * Composed type loader that iterates through array of available loaders
 *
 * @author xxDark
 */
final class ComposedTypeLoader implements ITypeLoader {
    private final Collection<ITypeLoader> loaders;

    ComposedTypeLoader(Collection<ITypeLoader> loaders) {
        this.loaders = loaders;
    }

    @Override
    public boolean tryLoadType(String s, Buffer buffer) {
        for (ITypeLoader loader : this.loaders) {
            if (loader.tryLoadType(s, buffer)) return true;
            buffer.reset();
        }
        return false;
    }
}
