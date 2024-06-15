package software.coley.recaf.services.decompile.fallback;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.decompile.AbstractJvmDecompiler;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.services.decompile.fallback.print.ClassPrinter;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Fallback decompiler implementation.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class FallbackDecompiler extends AbstractJvmDecompiler {
	public static final String NAME = "Fallback";
	private static final String VERSION = "1.0.0";
	private final TextFormatConfig formatConfig;

	/**
	 * New Procyon decompiler instance.
	 *
	 * @param config
	 * 		Config instance.
	 */
	@Inject
	public FallbackDecompiler(@Nonnull FallbackConfig config, @Nonnull TextFormatConfig formatConfig) {
		super(NAME, VERSION, config);
		this.formatConfig = formatConfig;
	}

	@Nonnull
	@Override
	protected DecompileResult decompileInternal(@Nonnull Workspace workspace, @Nonnull JvmClassInfo classInfo) {
		String decompile = new ClassPrinter(formatConfig, classInfo).print();
		int configHash = getConfig().getHash();
		return new DecompileResult(decompile, configHash);
	}
}
