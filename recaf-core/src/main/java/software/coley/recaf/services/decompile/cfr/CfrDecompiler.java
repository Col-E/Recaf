package software.coley.recaf.services.decompile.cfr;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.util.CfrVersionInfo;
import org.benf.cfr.reader.util.DecompilerComment;
import software.coley.recaf.services.decompile.AbstractJvmDecompiler;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.util.ReflectUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.lang.reflect.Field;
import java.util.Collections;

/**
 * CFR decompiler implementation.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class CfrDecompiler extends AbstractJvmDecompiler {
	public static final String NAME = "CFR";
	private final CfrConfig config;

	/**
	 * New CFR decompiler instance.
	 *
	 * @param config
	 * 		Config instance.
	 */
	@Inject
	public CfrDecompiler(@Nonnull CfrConfig config) {
		super(NAME, CfrVersionInfo.VERSION, config);
		this.config = config;
	}

	@Override
	public DecompileResult decompile(@Nonnull Workspace workspace, @Nonnull String name, @Nonnull byte[] bytecode) {
		ClassSource source = new ClassSource(workspace, name, bytecode);
		SinkFactoryImpl sink = new SinkFactoryImpl();
		CfrDriver driver = new CfrDriver.Builder()
				.withClassFileSource(source)
				.withOutputSink(sink)
				.withOptions(config.toMap())
				.build();
		driver.analyse(Collections.singletonList(name));
		String decompile = sink.getDecompilation();
		int configHash = getConfig().getConfigHash();
		if (decompile == null)
			return new DecompileResult(null, sink.getException(), DecompileResult.ResultType.FAILURE, configHash);
		return new DecompileResult(filter(decompile), null, DecompileResult.ResultType.SUCCESS, configHash);
	}

	@Override
	public CfrConfig getConfig() {
		return (CfrConfig) super.getConfig();
	}

	private static String filter(String decompile) {
		// CFR emits a 'Decompiled with CFR' header, which is annoying, so we'll remove that.
		int commentStart = decompile.indexOf("/*\n");
		int commentEnd = decompile.indexOf(" */\n");
		if (commentStart >= 0 && commentEnd > commentStart)
			decompile = decompile.substring(0, commentStart) + decompile.substring(commentEnd + 4);
		return decompile;
	}

	static {
		try {
			// Rewrite CFR comments to not say "use --option" since this is not a command line context.
			Field field = ReflectUtil.getDeclaredField(DecompilerComment.class, "comment");
			ReflectUtil.quietSet(DecompilerComment.RENAME_MEMBERS, field, "Duplicate member names detected");
			ReflectUtil.quietSet(DecompilerComment.ILLEGAL_IDENTIFIERS, field, "Illegal identifiers detected");
			ReflectUtil.quietSet(DecompilerComment.MALFORMED_SWITCH, field, "Recovered potentially malformed switches");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
