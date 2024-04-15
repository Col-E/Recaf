package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.decompile.filter.OutputTextFilter;

import java.util.HashSet;
import java.util.Set;

/**
 * Base for {@link Decompiler}.
 *
 * @author Matt Coley
 */
public abstract class AbstractDecompiler implements Decompiler {
	protected final Set<OutputTextFilter> textFilters = new HashSet<>();
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

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	@Override
	public String getVersion() {
		return version;
	}

	@Nonnull
	@Override
	public DecompilerConfig getConfig() {
		return config;
	}

	@Override
	public boolean addOutputTextFilter(@Nonnull OutputTextFilter filter) {
		return textFilters.add(filter);
	}

	@Override
	public boolean removeOutputTextFilter(@Nonnull OutputTextFilter filter) {
		return textFilters.remove(filter);
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
