package software.coley.recaf.services.decompile.procyon;

import com.strobel.Procyon;
import com.strobel.assembler.metadata.*;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.services.decompile.AbstractJvmDecompiler;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.workspace.model.Workspace;

import java.io.StringWriter;

/**
 * Procyon decompiler implementation.
 *
 * @author xDark
 */
@ApplicationScoped
public class ProcyonDecompiler extends AbstractJvmDecompiler {
	public static final String NAME = "Procyon";
	private final ProcyonConfig config;

	/**
	 * New Procyon decompiler instance.
	 *
	 * @param config
	 * 		Config instance.
	 */
	@Inject
	public ProcyonDecompiler(@Nonnull ProcyonConfig config) {
		super(NAME, Procyon.version(), config);
		this.config = config;
	}

	@Override
	public DecompileResult decompile(@Nonnull Workspace workspace, @Nonnull String name, @Nonnull byte[] bytecode) {
		ITypeLoader loader = new CompositeTypeLoader(
				new TargetedTypeLoader(name, bytecode),
				new WorkspaceTypeLoader(workspace)
		);
		DecompilerSettings settings = config.toSettings();
		settings.setTypeLoader(loader);
		MetadataSystem system = new MetadataSystem(loader);
		TypeReference ref = system.lookupType(name);
		DecompilationOptions decompilationOptions = new DecompilationOptions();
		decompilationOptions.setSettings(settings);
		StringWriter writer = new StringWriter();
		settings.getLanguage().decompileType(ref.resolve(), new PlainTextOutput(writer), decompilationOptions);
		String decompile = writer.toString();
		int configHash = getConfig().getConfigHash();
		if (decompile == null)
			return new DecompileResult(null, new IllegalStateException("Missing decompilation output"), DecompileResult.ResultType.FAILURE, configHash);
		return new DecompileResult(decompile, null, DecompileResult.ResultType.SUCCESS, configHash);
	}

	/**
	 * Type loader to load a single class file.
	 * Used as the first loader within a {@link CompositeTypeLoader} such that it overrides any
	 * following type loader that could also procure the same class info.
	 */
	private record TargetedTypeLoader(String name, byte[] data) implements ITypeLoader {
		@Override
		public boolean tryLoadType(String internalName, Buffer buffer) {
			if (internalName.equals(name)) {
				byte[] data = this.data;
				buffer.position(0);
				buffer.putByteArray(data, 0, data.length);
				buffer.position(0);
				return true;
			}
			return false;
		}
	}
}
