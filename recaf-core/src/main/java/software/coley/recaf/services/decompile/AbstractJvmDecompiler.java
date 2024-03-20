package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.ClassReader;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.builtin.CachedDecompileProperty;
import software.coley.recaf.workspace.model.Workspace;

import java.util.HashSet;
import java.util.Set;

/**
 * Basic setup for {@link JvmDecompiler}.
 *
 * @author Matt Coley
 */
public abstract class AbstractJvmDecompiler extends AbstractDecompiler implements JvmDecompiler {
	private final Set<JvmBytecodeFilter> bytecodeFilters = new HashSet<>();

	/**
	 * @param name
	 * 		Decompiler name.
	 * @param version
	 * 		Decompiler version.
	 * @param config
	 * 		Decompiler configuration.
	 */
	public AbstractJvmDecompiler(@Nonnull String name, @Nonnull String version, @Nonnull DecompilerConfig config) {
		super(name, version, config);
	}

	@Override
	public boolean addJvmBytecodeFilter(@Nonnull JvmBytecodeFilter filter) {
		return bytecodeFilters.add(filter);
	}

	@Override
	public boolean removeJvmBytecodeFilter(@Nonnull JvmBytecodeFilter filter) {
		return bytecodeFilters.add(filter);
	}

	@Nonnull
	@Override
	public final DecompileResult decompile(@Nonnull Workspace workspace, @Nonnull JvmClassInfo classInfo) {
		// Check for cached result, returning the cached result if found
		// and only if the current config matches the one that yielded the cached result.
		DecompileResult cachedResult = CachedDecompileProperty.get(classInfo, this);
		if (cachedResult != null) {
			if (cachedResult.getConfigHash() == getConfig().getHash())
				return cachedResult;

			// Config changed, void the cache.
			CachedDecompileProperty.remove(classInfo);
		}

		// Get bytecode and run through filters.
		JvmClassInfo filteredBytecode;
		if (bytecodeFilters.isEmpty()) {
			filteredBytecode = classInfo;
		} else {
			boolean dirty = false;
			byte[] bytecode = classInfo.getBytecode();
			for (JvmBytecodeFilter filter : bytecodeFilters) {
				byte[] filtered = filter.filter(workspace, classInfo, bytecode);
				if (filtered != bytecode) {
					bytecode = filtered;
					dirty = true;
				}
			}
			filteredBytecode = dirty ? classInfo.toJvmClassBuilder().adaptFrom(bytecode).build() : classInfo;
		}

		// Pass to implementation.
		DecompileResult result = decompileInternal(workspace, filteredBytecode);

		// Adapt output decompilation if output filters are registered.
		if (result.getType() == DecompileResult.ResultType.SUCCESS && result.getText() != null && !textFilters.isEmpty()) {
			String text = result.getText();
			for (OutputTextFilter filter : textFilters)
				text = filter.filter(workspace, classInfo, text);
			result = result.withText(text);
		}

		// Cache result.
		CachedDecompileProperty.set(classInfo, this, result);
		return result;
	}

	/**
	 * Takes on the work of {@link #decompile(Workspace, JvmClassInfo)} after the {@link #bytecodeFilters} have been applied to the class.
	 *
	 * @param workspace
	 * 		Workspace to pull data from.
	 * @param classInfo
	 * 		Class to decompile.
	 *
	 * @return Decompilation result.
	 */
	@Nonnull
	protected abstract DecompileResult decompileInternal(@Nonnull Workspace workspace, @Nonnull JvmClassInfo classInfo);

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		AbstractJvmDecompiler other = (AbstractJvmDecompiler) o;

		return bytecodeFilters.equals(other.bytecodeFilters);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + bytecodeFilters.hashCode();
		return result;
	}
}
