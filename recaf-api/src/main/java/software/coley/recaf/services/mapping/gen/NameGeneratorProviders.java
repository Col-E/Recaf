package software.coley.recaf.services.mapping.gen;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import software.coley.recaf.services.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Service managing available {@link NameGeneratorProvider} types.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class NameGeneratorProviders implements Service {
	public static final String SERVICE_ID = "name-gen-providers";
	private final NameGeneratorProvidersConfig config;
	private final Map<String, NameGeneratorProvider<?>> providerMap = new HashMap<>();

	@Inject
	public NameGeneratorProviders(@Nonnull NameGeneratorProvidersConfig config,
								  @Nonnull Instance<NameGeneratorProvider<?>> providers) {
		this.config = config;

		for (NameGeneratorProvider<?> provider : providers)
			providerMap.put(provider.getId(), provider);
	}

	/**
	 * @param provider
	 * 		New provider to add.
	 */
	public void registerProvider(@Nonnull NameGeneratorProvider<?> provider) {
		providerMap.put(provider.getId(), provider);
	}

	/**
	 * @return Map of available providers.
	 */
	@Nonnull
	public Map<String, NameGeneratorProvider<?>> getProviders() {
		return Collections.unmodifiableMap(providerMap);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public NameGeneratorProvidersConfig getServiceConfig() {
		return config;
	}
}
