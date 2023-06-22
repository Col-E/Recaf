package software.coley.recaf.services.decompile;

import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;

/**
 * Dummy config for {@link NoopJvmDecompiler} and {@link NoopAndroidDecompiler}.
 *
 * @author Matt Coley
 */
public class NoopDecompilerConfig extends BasicConfigContainer implements DecompilerConfig {
	/**
	 * New dummy config.
	 */
	public NoopDecompilerConfig() {
		super(ConfigGroups.SERVICE_DECOMPILE, "decompiler-noop" + CONFIG_SUFFIX);
	}

	@Override
	public int getConfigHash() {
		return 0;
	}

	@Override
	public void setConfigHash(int hash) {
		// no-op
	}
}