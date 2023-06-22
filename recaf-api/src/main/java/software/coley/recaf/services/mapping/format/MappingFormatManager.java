package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import software.coley.recaf.services.Service;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Manager of supported {@link MappingFileFormat} implementations.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class MappingFormatManager implements Service {
	public static final String SERVICE_ID = "mapping-formats";
	private final Map<String, Supplier<MappingFileFormat>> formatProviderMap = new TreeMap<>();
	private final MappingFormatManagerConfig config;

	/**
	 * @param config
	 * 		Config to pull values from.
	 * @param implementations
	 * 		CDI provider of mapping format implementations.
	 */
	@Inject
	public MappingFormatManager(MappingFormatManagerConfig config,
								Instance<MappingFileFormat> implementations) {
		this.config = config;

		// Register implementations from CDI
		// The formats themselves are @Dependent meaning we get will use the handle's as suppliers
		for (Instance.Handle<MappingFileFormat> handle : implementations.handles()) {
			MappingFileFormat format = handle.get();
			registerFormat(format.implementationName(), handle::get);
		}
	}

	/**
	 * @return Set of all known file formats by name.
	 */
	@Nonnull
	public Set<String> getMappingFileFormats() {
		return formatProviderMap.keySet();
	}

	/**
	 * @param name
	 * 		Name of format.
	 *
	 * @return Instance of the file format, or {@code null} if none were found matching the name.
	 */
	@Nullable
	public MappingFileFormat createFormatInstance(String name) {
		Supplier<MappingFileFormat> supplier = formatProviderMap.get(name);
		if (supplier != null)
			return supplier.get();
		return null;
	}

	/**
	 * @param name
	 * 		The format name.
	 * @param supplier
	 * 		A supplier to provide new instances of the format.
	 */
	public void registerFormat(@Nonnull String name, @Nonnull Supplier<MappingFileFormat> supplier) {
		formatProviderMap.put(name, supplier);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public MappingFormatManagerConfig getServiceConfig() {
		return config;
	}
}
