package software.coley.recaf.services.decompile.fallback;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.services.decompile.BaseDecompilerConfig;
import software.coley.recaf.util.ExcludeFromJacocoGeneratedReport;

/**
 * Config for {@link FallbackDecompiler}
 *
 * @author Matt Coley
 */
@ApplicationScoped
@ExcludeFromJacocoGeneratedReport(justification = "Config POJO")
public class FallbackConfig extends BaseDecompilerConfig {
	@Inject
	public FallbackConfig() {
		super("decompiler-fallback" + CONFIG_SUFFIX);
		registerConfigValuesHashUpdates();
	}
}
