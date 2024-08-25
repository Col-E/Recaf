package software.coley.recaf.services.decompile.vineflower;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.decompile.AbstractJvmDecompiler;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Vineflower decompiler implementation.
 *
 * @author therathatter
 */
@ApplicationScoped
public class VineflowerDecompiler extends AbstractJvmDecompiler {
	public static final String NAME = "Vineflower";
	private final VineflowerConfig config;
	private final IFernflowerLogger logger;
	private final IResultSaver dummySaver = new DummyResultSaver();

	/**
	 * New Vineflower decompiler instance.
	 *
	 * @param config
	 * 		Decompiler configuration.
	 */
	@Inject
	public VineflowerDecompiler(@Nonnull VineflowerConfig config) {
		// Change this version to be dynamic when / if the Vineflower authors make a function that returns the version...
		super(NAME, "1.10.1", config);
		this.config = config;
		logger = new VineflowerLogger(config);
	}

	@Nonnull
	@Override
	public DecompileResult decompileInternal(@Nonnull Workspace workspace, @Nonnull JvmClassInfo info) {
		Fernflower fernflower = new Fernflower(dummySaver, config.getFernflowerProperties(), logger);

		try {
			ClassSource source = new ClassSource(workspace, info);
			fernflower.addSource(source);
			fernflower.addLibrary(new LibrarySource(workspace, info));
			fernflower.decompileContext();

			String decompiled = source.getSink().getDecompiledOutput().get();

			if (decompiled == null || decompiled.isEmpty()) {
				return new DecompileResult(new IllegalStateException("Missing decompilation output"), config.getHash());
			}

			return new DecompileResult(decompiled, config.getHash());
		} catch (Exception e) {
			return new DecompileResult(e, config.getHash());
		}
	}
}
