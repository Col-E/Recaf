package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.Workspace;

/**
 * No-op decompiler for {@link JvmDecompiler}
 *
 * @author Matt Coley
 */
public class NoopJvmDecompiler extends AbstractJvmDecompiler {
	private static final NoopJvmDecompiler INSTANCE = new NoopJvmDecompiler();

	private NoopJvmDecompiler() {
		super("no-op-jvm", "1.0.0", new NoopDecompilerConfig());
	}

	/**
	 * @return Singleton instance.
	 */
	public static NoopJvmDecompiler getInstance() {
		return INSTANCE;
	}

	@Override
	public DecompileResult decompile(@Nonnull Workspace workspace, @Nonnull String name, @Nonnull byte[] bytecode) {
		return new DecompileResult(null, null, DecompileResult.ResultType.SKIPPED, getConfig().getConfigHash());
	}
}
