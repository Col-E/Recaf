package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;

/**
 * Basic setup for {@link AndroidDecompiler}.
 *
 * @author Matt Coley
 */
public abstract class AbstractAndroidDecompiler extends AbstractDecompiler implements AndroidDecompiler {
	/**
	 * @param name
	 * 		Decompiler name.
	 * @param version
	 * 		Decompiler version.
	 * @param config
	 * 		Decompiler configuration.
	 */
	public AbstractAndroidDecompiler(@Nonnull String name, @Nonnull String version, @Nonnull DecompilerConfig config) {
		super(name, version, config);
	}
}
