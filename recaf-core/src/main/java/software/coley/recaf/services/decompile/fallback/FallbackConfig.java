package software.coley.recaf.services.decompile.fallback;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.services.decompile.BaseDecompilerConfig;

/**
 * Config for {@link FallbackDecompiler}
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class FallbackConfig extends BaseDecompilerConfig {
	@Inject
	public FallbackConfig() {
		super("decompiler-fallback" + CONFIG_SUFFIX);
		registerConfigValuesHashUpdates();
	}
}
