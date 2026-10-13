package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.phantom.GeneratedPhantomWorkspaceResource;
import software.coley.recaf.services.phantom.model.PhantomClassConstraint;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.bundle.VersionedJvmClassBundle;
import software.coley.recaf.workspace.model.resource.RuntimeWorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

/**
 * Shared class/type lookup utility for phantom analysis stages.
 *
 * @author Matt Coley
 */
public class ClassLookup {
	private final Workspace workspace;
	private final Map<String, JvmClassInfo> providedClasses;
	private final Map<String, ClassInfo> knownClasses = new HashMap<>();
	private final Set<String> missingClasses = new HashSet<>();
	private final Map<String, Class<?>> platformTypes = new HashMap<>();
	private final Set<String> unavailablePlatformTypes = new HashSet<>();
	private final int targetVersion;
	private final Map<String, PhantomClassConstraint> constraints;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 * @param providedClasses
	 * 		Input classes currently being analyzed.
	 * @param constraints
	 * 		Shared phantom constraints for the current analysis run.
	 * @param targetVersion
	 * 		Java version whose multi-release classes should be selected.
	 */
	public ClassLookup(@Nonnull Workspace workspace,
	                   @Nonnull Map<String, JvmClassInfo> providedClasses,
	                   @Nonnull Map<String, PhantomClassConstraint> constraints,
	                   int targetVersion) {
		this.workspace = workspace;
		this.providedClasses = new HashMap<>(providedClasses);
		this.constraints = constraints;
		this.targetVersion = targetVersion;
	}

	/**
	 * @param internalName
	 * 		Internal class name.
	 *
	 * @return {@code true} when the type is already known from the inputs, workspace, or runtime classes.
	 */
	public boolean isKnown(@Nullable String internalName) {
		return isSystemType(internalName) || getKnownClassInfo(internalName) != null;
	}

	/**
	 * @param internalName
	 * 		Internal class name.
	 *
	 * @return Known class info, or {@code null} when it must be modeled as a phantom.
	 */
	@Nullable
	public ClassInfo getKnownClassInfo(@Nullable String internalName) {
		if (internalName == null)
			return null;

		// Provided classes have priority over workspace classes, since they are the ones currently being analyzed.
		JvmClassInfo provided = providedClasses.get(internalName);
		if (provided != null)
			return provided;

		// Check the cache of known classes, then check the cache of missing classes.
		ClassInfo cached = knownClasses.get(internalName);
		if (cached != null)
			return cached;
		if (missingClasses.contains(internalName))
			return null;

		// Lookup the class in the workspace, and cache the result (or lack thereof).
		ClassPathNode path = findJvmClass(internalName);
		if (path == null) {
			missingClasses.add(internalName);
			return null;
		}
		cached = path.getValue();
		knownClasses.put(internalName, cached);
		return cached;
	}

	/**
	 * @param internalName
	 * 		Class name.
	 *
	 * @return Kind inferred from known class info or collected phantom evidence.
	 */
	@Nonnull
	public PhantomTypeKind kindOf(@Nonnull String internalName) {
		// First check for any collected phantom evidence, then fall back to known class lookup.
		PhantomClassConstraint constraint = constraints.get(internalName);
		if (constraint != null) {
			if (constraint.isAnnotation())
				return PhantomTypeKind.ANNOTATION;
			if (constraint.isEnum())
				return PhantomTypeKind.ENUM;
			if (constraint.isInterface())
				return PhantomTypeKind.INTERFACE;
			if (constraint.hasClassEvidence() || !constraint.getRequiredSupertypes().isEmpty())
				return PhantomTypeKind.CLASS;
		}

		// No phantom evidence, infer from known class info.
		ClassInfo info = getKnownClassInfo(internalName);
		if (info == null) {
			if (!isSystemType(internalName))
				return PhantomTypeKind.UNKNOWN;
			return platformKind(getPlatformClass(internalName));
		}
		if (info.hasEnumModifier())
			return PhantomTypeKind.ENUM;
		if (info.hasAnnotationModifier())
			return PhantomTypeKind.ANNOTATION;
		return info.hasInterfaceModifier() ? PhantomTypeKind.INTERFACE : PhantomTypeKind.CLASS;
	}

	/**
	 * @param internalName
	 * 		Internal name of a known or platform type.
	 *
	 * @return Superclass of the type, or {@code null} when the type is an interface, has no superclass,
	 * or is neither known nor available from the platform.
	 */
	@Nullable
	public String getKnownSuperName(@Nonnull String internalName) {
		ClassInfo info = getKnownClassInfo(internalName);
		if (info != null)
			return info.hasInterfaceModifier() ? null : info.getSuperName();

		Class<?> type = getPlatformClass(internalName);
		if (type == null || type.isInterface() || type.getSuperclass() == null)
			return null;

		return type.getSuperclass().getName().replace('.', '/');
	}

	/**
	 * @param internalName
	 * 		Internal name of a known or platform type.
	 *
	 * @return Interfaces the type directly implements or extends.
	 * Empty when the type is neither known nor available from the platform.
	 */
	@Nonnull
	public Set<String> getKnownInterfaces(@Nonnull String internalName) {
		Set<String> result = new TreeSet<>();
		ClassInfo info = getKnownClassInfo(internalName);
		if (info != null) {
			result.addAll(info.getInterfaces());
			return result;
		}

		Class<?> type = getPlatformClass(internalName);
		if (type != null)
			for (Class<?> implemented : type.getInterfaces())
				result.add(implemented.getName().replace('.', '/'));

		return result;
	}

	/**
	 * @param internalName
	 * 		Internal name of a known or platform type.
	 *
	 * @return {@code true} when the type is a {@code final} class, so it cannot be extended.
	 */
	public boolean isKnownFinal(@Nonnull String internalName) {
		ClassInfo info = getKnownClassInfo(internalName);
		if (info != null)
			return info.hasFinalModifier();

		Class<?> type = getPlatformClass(internalName);
		return type != null && !type.isInterface() && Modifier.isFinal(type.getModifiers());
	}

	/**
	 * @param internalName
	 * 		Internal name of a known or platform type.
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 *
	 * @return {@code true} when the type itself declares the method as a {@code final} instance method. Inherited
	 * methods are not considered.
	 */
	public boolean declaresFinalInstanceMethod(@Nonnull String internalName, @Nonnull String name,
	                                           @Nonnull String descriptor) {
		ClassInfo info = getKnownClassInfo(internalName);
		if (info != null) {
			for (MethodMember method : info.getMethods())
				if (method.getName().equals(name) && method.getDescriptor().equals(descriptor)
						&& method.hasFinalModifier() && !method.hasStaticModifier())
					return true;
			return false;
		}

		Class<?> type = getPlatformClass(internalName);
		if (type == null)
			return false;

		try {
			for (Method method : type.getDeclaredMethods())
				if (method.getName().equals(name) && Type.getMethodDescriptor(method).equals(descriptor)
						&& Modifier.isFinal(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()))
					return true;
		} catch (LinkageError ignored) {
			// Platform type with unloadable signature types. Treat the method as not final.
		}
		return false;
	}

	/**
	 * @param child
	 * 		Potential subtype.
	 * @param target
	 * 		Potential supertype.
	 *
	 * @return {@code true} when {@code child} is a <i>strict</i> subtype of {@code target}.
	 */
	public boolean isStrictSubtypeOf(@Nullable String child, @Nullable String target) {
		return child != null
				&& target != null
				&& !child.equals(target) // strict subtyping means the types cannot be the same.
				&& isSubtypeOf(child, target, new HashSet<>());
	}

	private boolean isSubtypeOf(@Nullable String child,
	                            @Nullable String target,
	                            @Nonnull Set<String> visited) {
		// Base cases for null handling, equality, and cycle prevention.
		if (child == null || target == null)
			return false;
		if (child.equals(target))
			return true;
		if (!visited.add(child))
			return false;

		// Check if the child is a phantom with known constraints, and if so check its resolved supertype and interfaces.
		PhantomClassConstraint constraint = constraints.get(child);
		if (constraint != null) {
			if (isSubtypeOf(constraint.getResolvedSuperName(), target, visited))
				return true;
			for (String interfaceName : constraint.getResolvedInterfaces())
				if (isSubtypeOf(interfaceName, target, visited))
					return true;
			for (String requiredSupertype : constraint.getRequiredSupertypes())
				if (isSubtypeOf(requiredSupertype, target, visited))
					return true;
		}

		// If the child is not a phantom, check known class info for its supertype and interfaces.
		ClassInfo info = getKnownClassInfo(child);
		if (info == null)
			return false;
		if (isSubtypeOf(info.getSuperName(), target, visited))
			return true;
		for (String interfaceName : info.getInterfaces())
			if (isSubtypeOf(interfaceName, target, visited))
				return true;
		return false;
	}

	/**
	 * @param internalName
	 * 		Internal class name.
	 *
	 * @return Existing or newly created phantom constraint, or {@code null} when the type is known.
	 */
	@Nullable
	public PhantomClassConstraint getOrCreateConstraint(@Nonnull String internalName) {
		if (internalName.startsWith("["))
			return null;
		if (isKnown(internalName))
			return null;
		return constraints.computeIfAbsent(internalName, PhantomClassConstraint::new);
	}

	/**
	 * Adds a missing class name as a phantom candidate.
	 *
	 * @param internalName
	 * 		Internal class name.
	 */
	public void collectInternalName(@Nullable String internalName) {
		if (internalName == null)
			return;
		getOrCreateConstraint(internalName);
	}

	/**
	 * Adds any missing referenced classes within the given type.
	 *
	 * @param type
	 * 		Type to inspect.
	 */
	public void collectType(@Nonnull Type type) {
		switch (type.getSort()) {
			case Type.ARRAY -> collectType(type.getElementType());
			case Type.OBJECT -> collectInternalName(type.getInternalName());
			case Type.METHOD -> {
				collectType(type.getReturnType());
				for (Type argumentType : type.getArgumentTypes())
					collectType(argumentType);
			}
			default -> {
				// Primitive type, nothing to do.
			}
		}
	}

	/**
	 * Adds any missing referenced types from a field descriptor.
	 *
	 * @param descriptor
	 * 		Field descriptor.
	 */
	public void collectDescriptor(@Nonnull String descriptor) {
		collectType(Type.getType(descriptor));
	}

	/**
	 * Adds any missing referenced types from a method descriptor.
	 *
	 * @param descriptor
	 * 		Method descriptor.
	 */
	public void collectMethodDescriptor(@Nonnull String descriptor) {
		collectType(Type.getMethodType(descriptor));
	}

	/**
	 * Find the path to the most appropriate class in the workspace for the given internal name.
	 * When multi-release classes are present, the highest version not newer than the compiler target is selected.
	 *
	 * @param internalName
	 * 		Class name.
	 *
	 * @return Path to the class in the workspace, or {@code null} when it is not found.
	 */
	@Nullable
	private ClassPathNode findJvmClass(@Nonnull String internalName) {
		Queue<Collection<? extends WorkspaceResource>> resourceQueue = new ArrayDeque<>();
		Collection<? extends WorkspaceResource> resources = workspace.getAllResources(false);
		do {
			for (WorkspaceResource resource : resources) {
				// We should be skipping internal resources, but just in case...
				if (resource instanceof GeneratedPhantomWorkspaceResource || resource instanceof RuntimeWorkspaceResource)
					continue;

				// Check for multi-release classes, and if so, select the highest version not newer than the compiler target.
				if (targetVersion >= 9) {
					var entry = resource.getVersionedJvmClassBundles().floorEntry(targetVersion);
					while (entry != null) {
						VersionedJvmClassBundle bundle = entry.getValue();
						JvmClassInfo classInfo = bundle.get(internalName);
						if (classInfo != null)
							return PathNodes.classPath(workspace, resource, bundle, classInfo);
						entry = resource.getVersionedJvmClassBundles().lowerEntry(entry.getKey());
					}
				}

				// If no multi-release class was found, check the non-versioned bundles.
				for (JvmClassBundle bundle : resource.jvmClassBundles()) {
					JvmClassInfo classInfo = bundle.get(internalName);
					if (classInfo != null)
						return PathNodes.classPath(workspace, resource, bundle, classInfo);
				}

				// Queue up embedded resources for further searching.
				resourceQueue.add(resource.getEmbeddedResources().values());
			}
		} while ((resources = resourceQueue.poll()) != null);
		return null;
	}

	/**
	 * @param internalName
	 * 		Internal name of a type available from the platform class loader.
	 *
	 * @return Loaded platform type, or {@code null} if it is unavailable.
	 */
	@Nullable
	public Class<?> getPlatformClass(@Nonnull String internalName) {
		Class<?> type = platformTypes.get(internalName);
		if (type != null)
			return type;
		if (unavailablePlatformTypes.contains(internalName))
			return null;

		try {
			type = Class.forName(internalName.replace('/', '.'), false, ClassLoader.getPlatformClassLoader());
			platformTypes.put(internalName, type);
			return type;
		} catch (ClassNotFoundException | LinkageError | SecurityException | IllegalArgumentException ex) {
			unavailablePlatformTypes.add(internalName);
			return null;
		}
	}

	/**
	 * @param type
	 * 		Platform type.
	 *
	 * @return Kind of the platform type.
	 */
	@Nonnull
	private static PhantomTypeKind platformKind(@Nullable Class<?> type) {
		if (type == null)
			return PhantomTypeKind.CLASS;
		if (type.isAnnotation())
			return PhantomTypeKind.ANNOTATION;
		if (type.isEnum())
			return PhantomTypeKind.ENUM;
		return type.isInterface() ? PhantomTypeKind.INTERFACE : PhantomTypeKind.CLASS;
	}

	/**
	 * @param internalName
	 * 		Internal class name.
	 *
	 * @return {@code true} when the type is a system type (from the JDK or platform class loader).
	 */
	private boolean isSystemType(@Nullable String internalName) {
		return internalName != null && (internalName.startsWith("java/") || getPlatformClass(internalName) != null);
	}
}
