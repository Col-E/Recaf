package software.coley.recaf.services.decompile;

import software.coley.recaf.util.ExcludeFromJacocoGeneratedReport;

/**
 * Dummy config for {@link NoopJvmDecompiler} and {@link NoopAndroidDecompiler}.
 *
 * @author Matt Coley
 */
@ExcludeFromJacocoGeneratedReport(justification = "Config POJO")
public class NoopDecompilerConfig extends BaseDecompilerConfig implements DecompilerConfig {
	/**
	 * New dummy config.
	 */
	public NoopDecompilerConfig() {
		super("noop");
	}

	@Override
	public int getHash() {
		return 0;
	}

	@Override
	public void setHash(int hash) {
		// no-op
	}
}