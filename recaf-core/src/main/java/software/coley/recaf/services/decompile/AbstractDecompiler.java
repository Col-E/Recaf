package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;

/**
 * Base for {@link Decompiler}.
 *
 * @author Matt Coley
 */
public class AbstractDecompiler implements Decompiler {
	private final String name;
	private final String version;
	private final DecompilerConfig config;

	/**
	 * @param name
	 * 		Decompiler name.
	 * @param version
	 * 		Decompiler version.
	 * @param config
	 * 		Decompiler configuration.
	 */
	public AbstractDecompiler(@Nonnull String name, @Nonnull String version, @Nonnull DecompilerConfig config) {
		this.name = name;
		this.version = version;
		this.config = config;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public DecompilerConfig getConfig() {
		return config;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AbstractDecompiler that = (AbstractDecompiler) o;

		if (!name.equals(that.name)) return false;
		if (!version.equals(that.version)) return false;
		return config.equals(that.config);
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + version.hashCode();
		result = 31 * result + config.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return getName() + " - " + getVersion();
	}
}
