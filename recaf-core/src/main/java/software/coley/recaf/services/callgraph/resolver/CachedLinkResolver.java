package software.coley.recaf.services.callgraph.resolver;

import dev.xdark.jlinker.LinkResolver;
import dev.xdark.jlinker.MethodDescriptorString;
import dev.xdark.jlinker.MethodResolutionException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.MemoizedFunctions;

import java.util.Optional;
import java.util.function.Function;

/**
 * Memoized wrapper of {@link LinkResolver}.
 *
 * @author Amejonah
 */
class CachedLinkResolver {
	private final LinkResolver backedResolver = LinkResolver.create();
	private final Function<MethodLookup, Optional<LinkedClass.LinkedMethod>> staticMethodResolver = MemoizedFunctions.memoize(
			key -> resolve(() -> backedResolver.resolveStaticMethod(key.owner(), key.name(), MethodDescriptorString.of(key.descriptor())))
	);
	private final Function<MethodLookup, Optional<LinkedClass.LinkedMethod>> virtualMethodResolver = MemoizedFunctions.memoize(
			key -> resolve(() -> backedResolver.resolveVirtualMethod(key.owner(), key.name(), MethodDescriptorString.of(key.descriptor())))
	);
	private final Function<MethodLookup, Optional<LinkedClass.LinkedMethod>> interfaceMethodResolver = MemoizedFunctions.memoize(
			key -> resolve(() -> backedResolver.resolveInterfaceMethod(key.owner(), key.name(), MethodDescriptorString.of(key.descriptor())))
	);
	private final Function<SpecialMethodLookup, Optional<LinkedClass.LinkedMethod>> specialMethodResolver = MemoizedFunctions.memoize(
			key -> resolve(() -> backedResolver.resolveSpecialMethod(key.owner(), key.name(), MethodDescriptorString.of(key.descriptor()), key.caller()))
	);

	@Nullable
	public LinkedClass.LinkedMethod resolveStaticMethod(@Nonnull LinkedClass owner, @Nonnull String name, @Nonnull String descriptor) {
		return staticMethodResolver.apply(new MethodLookup(owner, name, descriptor)).orElse(null);
	}

	@Nullable
	public LinkedClass.LinkedMethod resolveSpecialMethod(@Nonnull LinkedClass owner, @Nullable LinkedClass caller,
	                                                     @Nonnull String name, @Nonnull String descriptor) {
		return specialMethodResolver.apply(new SpecialMethodLookup(owner, caller, name, descriptor)).orElse(null);
	}

	@Nullable
	public LinkedClass.LinkedMethod resolveVirtualMethod(@Nonnull LinkedClass owner, @Nonnull String name, @Nonnull String descriptor) {
		return virtualMethodResolver.apply(new MethodLookup(owner, name, descriptor)).orElse(null);
	}

	@Nullable
	public LinkedClass.LinkedMethod resolveInterfaceMethod(@Nonnull LinkedClass owner, @Nonnull String name, @Nonnull String descriptor) {
		return interfaceMethodResolver.apply(new MethodLookup(owner, name, descriptor)).orElse(null);
	}

	@Nonnull
	private static Optional<LinkedClass.LinkedMethod> resolve(@Nonnull MethodSupplier supplier) {
		try {
			return Optional.of(supplier.get());
		} catch (MethodResolutionException ex) {
			return Optional.empty();
		}
	}

	@FunctionalInterface
	private interface MethodSupplier {
		@Nonnull
		LinkedClass.LinkedMethod get() throws MethodResolutionException;
	}

	private record MethodLookup(@Nonnull LinkedClass owner, @Nonnull String name, @Nonnull String descriptor) {}

	private record SpecialMethodLookup(@Nonnull LinkedClass owner, @Nullable LinkedClass caller,
	                                   @Nonnull String name, @Nonnull String descriptor) {}
}
