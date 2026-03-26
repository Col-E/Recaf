package software.coley.recaf.services.callgraph.resolver;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.services.callgraph.CallSite;
import software.coley.recaf.services.callgraph.ResolvedMethodCall;
import software.coley.recaf.workspace.model.Workspace;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Resolves call sites to concrete method declarations.
 *
 * @author Matt Coley
 */
public class CallResolver {
	private final CachedLinkResolver resolver = new CachedLinkResolver();
	private final Map<ClassInfo, LinkedClass> linkedClasses = Collections.synchronizedMap(new IdentityHashMap<>());
	private final ClassLookup lookup;

	public CallResolver(@Nonnull Workspace workspace) {
		lookup = new ClassLookup(workspace);
	}

	/**
	 * Resolves call sites to concrete method declarations.
	 *
	 * @author Matt Coley
	 */
	@Nullable
	public ResolvedMethodCall resolve(@Nonnull ClassInfo callingClass, @Nonnull CallSite callSite) {
		ClassInfo ownerClass = lookup.get(callSite.owner());
		if (ownerClass == null)
			return null;

		LinkedClass linkedOwnerClass = linked(ownerClass);
		LinkedClass.LinkedMethod resolution = switch (callSite.kind()) {
			case DIRECT -> resolveDirect(linkedOwnerClass, callSite);
			case SPECIAL ->
					resolver.resolveSpecialMethod(linkedOwnerClass, linked(callingClass), callSite.name(), callSite.descriptor());
			case STATIC -> resolver.resolveStaticMethod(linkedOwnerClass, callSite.name(), callSite.descriptor());
			case VIRTUAL -> resolver.resolveVirtualMethod(linkedOwnerClass, callSite.name(), callSite.descriptor());
			case INTERFACE -> resolver.resolveInterfaceMethod(linkedOwnerClass, callSite.name(), callSite.descriptor());
			case SUPER -> resolveSuper(linked(callingClass), linkedOwnerClass, callSite);
		};

		if (resolution == null)
			return null;
		return new ResolvedMethodCall(resolution.owner().innerValue(), resolution.method());
	}

	/**
	 * Invalidate cached data for a class that was removed from the workspace.
	 *
	 * @param classInfo
	 * 		Removed class.
	 */
	public void onClassRemoved(@Nonnull ClassInfo classInfo) {
		if (classInfo.isAndroidClass())
			linkedClasses.remove(classInfo);
	}

	@Nonnull
	private LinkedClass linked(@Nonnull ClassInfo classInfo) {
		return linkedClasses.computeIfAbsent(classInfo, c -> new LinkedClass(lookup, this::linked, c));
	}

	@Nullable
	private static LinkedClass.LinkedMethod resolveDirect(@Nonnull LinkedClass owner,
	                                                      @Nonnull CallSite callSite) {
		return owner.getMethod(callSite.name(), callSite.descriptor());
	}

	@Nullable
	private LinkedClass.LinkedMethod resolveSuper(@Nonnull LinkedClass caller,
	                                              @Nonnull LinkedClass symbolicOwner,
	                                              @Nonnull CallSite callSite) {
		if (Modifier.isInterface(symbolicOwner.accessFlags())) {
			LinkedClass targetInterface = findInterfaceInHierarchy(caller, symbolicOwner.name());
			if (targetInterface == null)
				return null;
			return resolver.resolveInterfaceMethod(targetInterface, callSite.name(), callSite.descriptor());
		}

		LinkedClass parent = caller.superClass();
		if (parent == null)
			return null;
		return resolver.resolveVirtualMethod(parent, callSite.name(), callSite.descriptor());
	}

	@Nullable
	private static LinkedClass findInterfaceInHierarchy(@Nonnull LinkedClass type, @Nonnull String interfaceName) {
		for (var itf : type.interfaces()) {
			if (itf.name().equals(interfaceName))
				return itf;
			LinkedClass nested = findInterfaceInHierarchy(itf, interfaceName);
			if (nested != null)
				return nested;
		}
		LinkedClass parent = type.superClass();
		if (parent == null)
			return null;
		return findInterfaceInHierarchy(parent, interfaceName);
	}
}
