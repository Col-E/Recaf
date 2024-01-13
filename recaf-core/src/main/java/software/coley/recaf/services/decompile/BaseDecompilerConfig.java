package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import software.coley.recaf.config.BasicConfigContainer;

/**
 * Base class for fields needed by all decompiler configurations
 *
 * @author therathatter
 */
public class BaseDecompilerConfig extends BasicConfigContainer implements DecompilerConfig {
    private int hash = 0;

    /**
     * @param group Container group.
     * @param id    Container ID.
     */
    public BaseDecompilerConfig(@Nonnull String group, @Nonnull String id) {
        super(group, id);
    }

    @Override
    public int getHash() {
        return hash;
    }

    @Override
    public void setHash(int hash) {
        this.hash = hash;
    }
}
