package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import software.coley.collections.Unchecked;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager for {@link InvokeDynamicResolver} instances.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class InvokeDynamicResolverManager {
	// Keep resolver order deterministic when several resolvers recognize the same call site.
	private static final Comparator<InvokeDynamicResolver> CMP = Comparator.comparingInt(InvokeDynamicResolver::getPriority)
			.thenComparing(resolver -> resolver.getClass().getName());
	private final Map<String, InvokeDynamicResolver> resolverMap = new HashMap<>();

	@Inject
	public InvokeDynamicResolverManager(@Nonnull Instance<InvokeDynamicResolver> resolvers) {
		// Register core resolvers.
		for (InvokeDynamicResolver resolver : resolvers)
			register(resolver);
	}

	/**
	 * @param resolvers
	 * 		List of resolvers to sort by priority.
	 *
	 * @return Sorted list of resolvers.
	 */
	@Nonnull
	public static List<InvokeDynamicResolver> sort(@Nonnull List<? extends InvokeDynamicResolver> resolvers) {
		resolvers = new ArrayList<>(resolvers); // Ensure the list is mutable.
		resolvers.sort(CMP);
		return Unchecked.cast(resolvers);
	}

	/**
	 * @return List of resolvers.
	 */
	@Nonnull
	public List<InvokeDynamicResolver> getResolvers() {
		return new ArrayList<>(resolverMap.values());
	}

	/**
	 * @param resolver
	 * 		Resolver to register.
	 */
	public void register(@Nonnull InvokeDynamicResolver resolver) {
		resolverMap.put(resolver.getClass().getName(), resolver);
	}

	/**
	 * @param resolverSource
	 * 		Source of resolvers to materialize into a list.
	 *
	 * @return Materialized list of resolvers.
	 */
	@Nonnull
	private static List<InvokeDynamicResolver> materializeResolvers(@Nonnull Iterable<InvokeDynamicResolver> resolverSource) {
		List<InvokeDynamicResolver> resolvers = new ArrayList<>();
		for (InvokeDynamicResolver resolver : resolverSource)
			resolvers.add(resolver);
		return resolvers;
	}
}
