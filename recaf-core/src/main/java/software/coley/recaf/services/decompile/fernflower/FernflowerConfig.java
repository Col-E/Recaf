package software.coley.recaf.services.decompile.fernflower;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.event.Level;
import software.coley.observables.ObservableObject;
import software.coley.recaf.services.decompile.BaseDecompilerConfig;

/**
 * FernflowerConfig
 *
 * @author meiMingle
 */
@ApplicationScoped
public class FernflowerConfig extends BaseDecompilerConfig {

    @Inject
    public FernflowerConfig() {
        super("decompiler-fernflower" + CONFIG_SUFFIX);
        registerConfigValuesHashUpdates();
    }

    public ObservableObject<Level> getLoggingLevel() {
        return new ObservableObject<>(Level.WARN);
    }
}
