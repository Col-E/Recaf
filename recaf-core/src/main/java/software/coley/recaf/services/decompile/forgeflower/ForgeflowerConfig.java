package software.coley.recaf.services.decompile.forgeflower;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.event.Level;
import software.coley.observables.ObservableObject;
import software.coley.recaf.services.decompile.BaseDecompilerConfig;

/**
 * ForgeflowerConfig
 *
 * @author meiMingle
 */
@ApplicationScoped
public class ForgeflowerConfig extends BaseDecompilerConfig {

    @Inject
    public ForgeflowerConfig() {
        super("decompiler-forgeflower" + CONFIG_SUFFIX);
        registerConfigValuesHashUpdates();
    }

    public ObservableObject<Level> getLoggingLevel() {
        return new ObservableObject<>(Level.WARN);
    }
}
