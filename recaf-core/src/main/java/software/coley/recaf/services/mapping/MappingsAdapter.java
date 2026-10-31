package software.coley.recaf.services.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.services.mapping.data.ClassMapping;
import software.coley.recaf.services.mapping.data.ClassMappingKey;
import software.coley.recaf.services.mapping.data.FieldMapping;
import software.coley.recaf.services.mapping.data.FieldMappingKey;
import software.coley.recaf.services.mapping.data.MappingKey;
import software.coley.recaf.services.mapping.data.MethodMapping;
import software.coley.recaf.services.mapping.data.MethodMappingKey;
import software.coley.recaf.services.mapping.data.VariableMapping;
import software.coley.recaf.services.mapping.data.VariableMappingKey;
import software.coley.recaf.util.NameTypePair;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * A {@link Mappings} implementation with a number of additional operations to support usage beyond basic mapping info storage.
 * <b>Enhancements</b>
 * <ol>
 * <li>Import mapping entries from a {@link IntermediateMappings} instance.</li>
 * <li>Enhance mapping entries with method family information from {@link InheritanceGraph}, see {@link #infillMethodFamilies(IntermediateMappings, InheritanceGraph)}</li>
 * <li>Enhance field/method lookups with inheritance info from {@link InheritanceGraph}, see {@link #enableHierarchyLookup(InheritanceGraph)}.</li>
 * <li>Enhance inner/outer class mapping edge cases via {@link #enableClassLookup(Workspace)}.</li>
 * <li>Adapt keys in cases where fields/vars do not have type info associated with them <i>(for formats that suck)</i>.</li>
 * </ol>
 *
 * @author Matt Coley
 */
public class MappingsAdapter implements Mappings {
	private static final int MAX_HIERARCHY_LOOKUP_CACHE_ENTRIES = 70_000; // Enough to fit a decent sized sample like a vanilla MC client jar.
	private static final Object NO_HIERARCHY_MAPPING = new Object();

	private final Map<MappingKey, String> mappings = new HashMap<>();
	private volatile HierarchyLookupCache inheritedFieldLookupCache;
	private volatile HierarchyLookupCache inheritedMethodLookupCache;
	private final boolean supportFieldTypeDifferentiation;
	private final boolean supportVariableTypeDifferentiation;
	private InheritanceGraph inheritanceGraph;
	private Workspace workspace;

	/**
	 * @param supportFieldTypeDifferentiation
	 *        {@code true} if the mapping format implementation includes type descriptors in field mappings.
	 * @param supportVariableTypeDifferentiation
	 *        {@code true} if the mapping format implementation includes type descriptors in variable mappings.
	 */
	public MappingsAdapter(boolean supportFieldTypeDifferentiation,
	                       boolean supportVariableTypeDifferentiation) {
		this.supportFieldTypeDifferentiation = supportFieldTypeDifferentiation;
		this.supportVariableTypeDifferentiation = supportVariableTypeDifferentiation;
	}

	/**
	 * Adds all the entries in the given mappings to the current mappings.
	 *
	 * @param mappings
	 * 		Intermediate mappings to add to the current mappings.
	 */
	public void importIntermediate(@Nonnull IntermediateMappings mappings) {
		for (String className : mappings.getClassesWithMappings()) {
			ClassMapping classMapping = mappings.getClassMapping(className);
			if (classMapping != null) {
				String oldClassName = classMapping.getOldName();
				String newClassName = classMapping.getNewName();
				if (!oldClassName.equals(newClassName))
					addClass(oldClassName, newClassName);
			}
			for (FieldMapping fieldMapping : mappings.getClassFieldMappings(className)) {
				String oldName = fieldMapping.getOldName();
				String newName = fieldMapping.getNewName();
				if (!oldName.equals(newName)) {
					if (doesSupportFieldTypeDifferentiation()) {
						addField(fieldMapping.getOwnerName(), oldName, fieldMapping.getDesc(), newName);
					} else {
						addField(fieldMapping.getOwnerName(), oldName, newName);
					}
				}
			}
			for (MethodMapping methodMapping : mappings.getClassMethodMappings(className)) {
				String oldMethodName = methodMapping.getOldName();
				String oldMethodDesc = methodMapping.getDesc();
				String newMethodName = methodMapping.getNewName();
				if (!oldMethodName.equals(newMethodName))
					addMethod(methodMapping.getOwnerName(), oldMethodName, oldMethodDesc, newMethodName);
			}
		}
		for (List<VariableMapping> variablesPerMethod : mappings.getVariables().values()) {
			for (VariableMapping variableMapping : variablesPerMethod) {
				addVariable(variableMapping.getOwnerName(), variableMapping.getMethodName(), variableMapping.getMethodDesc(),
						variableMapping.getOldName(), variableMapping.getDesc(), variableMapping.getIndex(),
						variableMapping.getNewName());
			}
		}
	}

	/**
	 * Enriches the provided mappings by normalizing method mappings across method families.
	 *
	 * @param inputMappings
	 * 		Original mappings to enrich.
	 * @param inheritanceGraph
	 * 		Inheritance graph to use for looking up method families.
	 */
	public void infillMethodFamilies(@Nonnull IntermediateMappings inputMappings, @Nonnull InheritanceGraph inheritanceGraph) {
		// Keep only one entry per method key while detecting conflicting explicit mappings.
		//
		// Mapping formats may preserve duplicate records, and some duplicates may even have conflicting output names.
		// We want to dedupe and abort on any conflicts first.
		Map<MethodMappingKey, MethodMapping> origins = new HashMap<>();
		for (List<MethodMapping> ownerMappings : inputMappings.getMethods().values()) {
			for (MethodMapping explicitMapping : ownerMappings) {
				MethodMappingKey key = new MethodMappingKey(explicitMapping.getOwnerName(), explicitMapping.getOldName(), explicitMapping.getDesc());
				MethodMapping previous = origins.putIfAbsent(key, explicitMapping);
				if (previous != null && !previous.getNewName().equals(explicitMapping.getNewName()))
					throw newMethodConflict(key, key.getOwner(), previous, explicitMapping);
			}
		}

		// Now gather all mappings and sort for deterministic processing order.
		List<MethodMapping> explicitMappings = origins.values().stream()
				.sorted(Comparator
						.comparing(MethodMapping::getOwnerName)
						.thenComparing(MethodMapping::getOldName)
						.thenComparing(MethodMapping::getDesc)
						.thenComparing(MethodMapping::getNewName))
				.toList();

		// Build a map of method names to descriptors that are explicitly mapped.
		Map<String, Set<String>> mappedDescriptorsByName = new HashMap<>();
		for (MethodMapping explicitMapping : explicitMappings)
			mappedDescriptorsByName.computeIfAbsent(explicitMapping.getOldName(), _ -> new HashSet<>()).add(explicitMapping.getDesc());

		// Iterate over the method mappings, and for each mapping,
		// check if there are any methods in the same override family that would be affected by the same mapping.
		// If so, add the same mapping for those methods as well.
		Map<InheritanceVertex, Set<InheritanceVertex>> familyCache = new IdentityHashMap<>(); // Thankfully we can get away with identity here.
		Map<InheritanceVertex, Map<NameTypePair, List<MethodFamilyEntry>>> familyMethodIndexCache = new IdentityHashMap<>();
		Map<MethodMappingKey, List<MethodFamilyEntry>> methodFamilyCache = new HashMap<>();
		Map<MethodMappingKey, String> infilledMethodNames = new HashMap<>();
		MethodFamilyRelationCache methodFamilyRelations = null;
		for (MethodMapping explicitMapping : explicitMappings) {
			// Skip constructors and static initializers.
			String methodName = explicitMapping.getOldName();
			if (!methodName.isEmpty() && methodName.charAt(0) == '<')
				continue;

			// Skip enriching mappings on library classes. Can't map those anyways.
			String ownerName = explicitMapping.getOwnerName();
			InheritanceVertex ownerVertex = inheritanceGraph.getVertex(ownerName);
			if (ownerVertex == null || ownerVertex.isLibraryVertex())
				continue;

			// Skip enriching mappings on private or static methods, as they cannot be inherited.
			ClassInfo ownerInfo = ownerVertex.getValue();
			MethodMember ownerMethod = ownerInfo.getDeclaredMethod(methodName, explicitMapping.getDesc());
			if (ownerMethod == null || ownerMethod.hasPrivateModifier() || ownerMethod.hasStaticModifier())
				continue;

			// Check if this method family has already been infilled by a previous explicit mapping.
			MethodMappingKey explicitKey = new MethodMappingKey(ownerName, methodName, explicitMapping.getDesc());
			String infilledName = infilledMethodNames.get(explicitKey);
			if (infilledName != null) {
				if (!infilledName.equals(explicitMapping.getNewName())) {
					MethodMapping previous = origins.get(explicitKey);
					if (previous == null || !previous.getNewName().equals(infilledName))
						previous = new MethodMapping(ownerName, methodName, explicitMapping.getDesc(), infilledName);
					throw newMethodConflict(explicitKey, ownerName, previous, explicitMapping);
				}

				// This same method family was already fully infilled by another explicit member.
				continue;
			}

			// Gather all methods in the same family as the explicit mapping.
			List<MethodFamilyEntry> methodFamily = methodFamilyCache.get(explicitKey);
			if (methodFamily == null) {
				Set<InheritanceVertex> family = familyCache.get(ownerVertex);
				if (family == null) {
					// Gather all vertices in the same family as the explicit mapping owner.
					family = inheritanceGraph.getVertexFamily(ownerName, false);
					familyCache.put(ownerVertex, family);
					for (InheritanceVertex familyVertex : family)
						familyCache.put(familyVertex, family);
				}

				// Build a map of method signatures to family members for the given method family.
				Map<NameTypePair, List<MethodFamilyEntry>> familyMethodIndex = familyMethodIndexCache.get(ownerVertex);
				if (familyMethodIndex == null) {
					familyMethodIndex = new HashMap<>();
					for (InheritanceVertex familyVertex : family) {
						if (familyVertex.isLibraryVertex())
							continue;

						ClassInfo familyInfo = familyVertex.getValue();
						for (MethodMember familyMethod : familyInfo.getMethods()) {
							Set<String> mappedDescriptors = mappedDescriptorsByName.get(familyMethod.getName());
							if (mappedDescriptors == null || !mappedDescriptors.contains(familyMethod.getDescriptor()))
								continue;

							NameTypePair signature = new NameTypePair(familyMethod.getName(), familyMethod.getDescriptor());
							familyMethodIndex.computeIfAbsent(signature, ignored -> new ArrayList<>())
									.add(new MethodFamilyEntry(familyVertex, familyInfo, familyMethod));
						}
					}

					for (InheritanceVertex familyVertex : family)
						familyMethodIndexCache.put(familyVertex, familyMethodIndex);
				}

				// Gather all family members that match the explicit mapping's name/desc.
				List<MethodFamilyEntry> candidates = familyMethodIndex.getOrDefault(new NameTypePair(methodName, explicitMapping.getDesc()), List.of());
				List<MethodFamilyEntry> matchingFamily = new ArrayList<>();
				if (methodFamilyRelations == null)
					methodFamilyRelations = new MethodFamilyRelationCache();
				for (MethodFamilyEntry candidate : candidates) {
					if (MappingsAdapter.isSameMethodFamily(ownerVertex, ownerInfo, ownerMethod, candidate.vertex(), candidate.ownerInfo(), candidate.method(), methodFamilyRelations))
						matchingFamily.add(candidate);
				}
				matchingFamily.sort(Comparator.comparing(entry -> entry.vertex().getName()));

				// Cache the family for later explicit mappings that may be in the same family.
				methodFamily = matchingFamily;
				methodFamilyCache.put(explicitKey, methodFamily);
				for (MethodFamilyEntry familyEntry : methodFamily) {
					MethodMappingKey familyKey = new MethodMappingKey(familyEntry.ownerInfo().getName(), methodName, explicitMapping.getDesc());
					methodFamilyCache.put(familyKey, methodFamily);
				}
			}

			// Apply the mapping once to the whole method family; later explicit entries in that family
			// can reuse this result without rescanning or rewriting every member.
			for (MethodFamilyEntry familyEntry : methodFamily) {
				String familyOwnerName = familyEntry.ownerInfo().getName();
				MethodMappingKey familyKey = new MethodMappingKey(familyOwnerName, methodName, explicitMapping.getDesc());
				String existing = getMappedMethodName(familyOwnerName, methodName, explicitMapping.getDesc());
				if (existing == null) {
					// Add a new mapping for this method family member.
					addMethod(familyOwnerName, methodName, explicitMapping.getDesc(), explicitMapping.getNewName());
					origins.put(familyKey, explicitMapping);
				} else if (!existing.equals(explicitMapping.getNewName())) {
					// If the names don't match, then we have a conflict between the existing mapping and the explicit mapping.
					// Shouldn't happen due to our earlier deduplication, but just in case...
					MethodMapping previous = origins.get(familyKey);
					if (previous == null)
						previous = new MethodMapping(familyOwnerName, methodName, explicitMapping.getDesc(), existing);
					throw newMethodConflict(familyKey, familyOwnerName, previous, explicitMapping);
				} else {
					origins.putIfAbsent(familyKey, explicitMapping);
				}

				// Record that this method family has been infilled with the explicit mapping's new name.
				infilledMethodNames.put(familyKey, explicitMapping.getNewName());
			}
		}
	}

	@Nullable
	@Override
	public String getMappedClassName(@Nonnull String internalName) {
		String mapped = mappings.get(getClassKey(internalName));
		if (mapped == null) {
			if (workspace != null) {
				// Pull the actual outer class name from the class-info in the workspace if available.
				ClassPathNode classPath = workspace.findClass(internalName);
				if (classPath != null) {
					ClassInfo info = classPath.getValue();
					String name = info.getName();
					String outerName = info.getOuterClassName();
					if (outerName != null && outerName.length() < name.length()) {
						String inner = name.substring(outerName.length() + 1);
						String outerMapped = getMappedClassName(outerName);
						if (outerMapped != null)
							mapped = outerMapped + "$" + inner;
					}
				}
			} else if (isInner(internalName)) {
				// We don't have a workspace, so the best we can do is assume standard 'Outer$Inner' conventions.
				int split = internalName.lastIndexOf("$");
				String inner = internalName.substring(split + 1);
				String outer = internalName.substring(0, split);
				String outerMapped = getMappedClassName(outer);
				if (outerMapped != null)
					mapped = outerMapped + "$" + inner;
			}
		}
		return mapped;
	}

	@Nullable
	@Override
	public String getMappedFieldName(@Nonnull String ownerName, @Nonnull String fieldName, @Nonnull String fieldDesc) {
		MappingKey key = getFieldKey(ownerName, fieldName, fieldDesc);
		String mapped = mappings.get(key);
		if (mapped == null && inheritanceGraph != null) {
			HierarchyLookupCache cache = fieldLookupCache();
			Object cached = cache.get(key);
			if (cached == null) {
				mapped = findInParent(ownerName, parent -> getFieldKey(parent, fieldName, fieldDesc));
				Object result = mapped == null ? NO_HIERARCHY_MAPPING : mapped;
				cached = cache.putIfSpace(key, result);
			}
			mapped = cached == NO_HIERARCHY_MAPPING ? null : (String) cached;
		}
		return mapped;
	}

	@Nullable
	@Override
	public String getMappedMethodName(@Nonnull String ownerName, @Nonnull String methodName, @Nonnull String methodDesc) {
		MappingKey key = getMethodKey(ownerName, methodName, methodDesc);
		String mapped = mappings.get(key);
		if (mapped == null && inheritanceGraph != null) {
			HierarchyLookupCache cache = methodLookupCache();
			Object cached = cache.get(key);
			if (cached == null) {
				mapped = findMethodInParent(ownerName, methodName, methodDesc);
				Object result = mapped == null ? NO_HIERARCHY_MAPPING : mapped;
				cached = cache.putIfSpace(key, result);
			}
			mapped = cached == NO_HIERARCHY_MAPPING ? null : (String) cached;
		}
		return mapped;
	}

	@Nullable
	@Override
	public String getMappedVariableName(@Nonnull String className, @Nonnull String methodName, @Nonnull String methodDesc,
	                                    @Nullable String name, @Nullable String desc, int index) {
		MappingKey key = getVariableKey(className, methodName, methodDesc, name, desc, index);
		return mappings.get(key);
	}

	@Nonnull
	@Override
	public IntermediateMappings exportIntermediate() {
		IntermediateMappings intermediate = new IntermediateMappings();
		for (Map.Entry<MappingKey, String> entry : new TreeMap<>(mappings).entrySet()) {
			MappingKey key = entry.getKey();
			String newName = entry.getValue();
			if (key instanceof ClassMappingKey ck) {
				intermediate.addClass(ck.getName(), newName);
			} else if (key instanceof MethodMappingKey mk) {
				intermediate.addMethod(mk.getOwner(), mk.getDesc(), mk.getName(), newName);
			} else if (key instanceof FieldMappingKey fk) {
				String oldOwner = fk.getOwner();
				String oldName = fk.getName();
				String oldDesc = fk.getDesc();
				intermediate.addField(oldOwner, oldDesc, oldName, newName);
			} else if (key instanceof VariableMappingKey vk) {
				intermediate.addVariable(vk.getOwner(), vk.getMethodName(), vk.getMethodDesc(),
						vk.getVariableDesc(), vk.getVariableName(), vk.getVariableIndex(), newName);
			}
		}
		return intermediate;
	}

	@Override
	public boolean doesSupportFieldTypeDifferentiation() {
		return supportFieldTypeDifferentiation;
	}

	@Override
	public boolean doesSupportVariableTypeDifferentiation() {
		return supportVariableTypeDifferentiation;
	}

	/**
	 * @return Lookup cache for field mappings that are inherited from parent classes.
	 */
	@Nonnull
	private HierarchyLookupCache fieldLookupCache() {
		HierarchyLookupCache cache = inheritedFieldLookupCache;
		if (cache == null) {
			synchronized (this) {
				cache = inheritedFieldLookupCache;
				if (cache == null)
					inheritedFieldLookupCache = cache = new HierarchyLookupCache();
			}
		}
		return cache;
	}

	/**
	 * @return Lookup cache for method mappings that are inherited from parent classes.
	 */
	@Nonnull
	private HierarchyLookupCache methodLookupCache() {
		HierarchyLookupCache cache = inheritedMethodLookupCache;
		if (cache == null) {
			synchronized (this) {
				cache = inheritedMethodLookupCache;
				if (cache == null)
					inheritedMethodLookupCache = cache = new HierarchyLookupCache();
			}
		}
		return cache;
	}

	/**
	 * @param owner
	 * 		Internal name of the class <i>"defining"</i> the member.
	 * 		<i>(Location in reference may not be where the member is actually defined, hence this lookup)</i>
	 * @param lookup
	 * 		Function that takes in the parent names of the given member owner class,
	 * 		and converts it to a member lookup key via {@link #getFieldKey(String, String, String)} or
	 *        {@link #getMethodKey(String, String, String)}.
	 *
	 * @return The first mapping match in a parent class found by the lookup function.
	 */
	@Nullable
	private String findInParent(String owner, Function<String, ? extends MappingKey> lookup) {
		InheritanceVertex vertex = inheritanceGraph.getVertex(owner);
		if (vertex == null)
			return null;
		Iterator<InheritanceVertex> iterator = vertex.allParents().iterator();
		while (iterator.hasNext()) {
			vertex = iterator.next();
			MappingKey key = lookup.apply(vertex.getName());
			String result = mappings.get(key);
			if (result != null)
				return result;
		}
		return null;
	}

	/**
	 * @param ownerName
	 * 		Internal name of the class <i>"defining"</i> the member.
	 * 		<i>(Location in reference may not be where the member is actually defined, hence this lookup)</i>
	 * @param methodName
	 * 		Name of the method.
	 * @param methodDesc
	 * 		Descriptor of the method.
	 *
	 * @return The first mapping match in a parent class with the same name/desc <i>(Some access restrictions apply)</i>.
	 */
	@Nullable
	private String findMethodInParent(@Nonnull String ownerName, @Nonnull String methodName, @Nonnull String methodDesc) {
		// Skip if not in workspace/inheritance graph.
		InheritanceVertex ownerVertex = inheritanceGraph.getVertex(ownerName);
		if (ownerVertex == null)
			return null;

		// Skip if the method is defined in the owner class and is private/static, as it would not be inherited.
		ClassInfo ownerInfo = ownerVertex.getValue();
		MethodMember ownerMethod = ownerInfo.getDeclaredMethod(methodName, methodDesc);
		if (ownerMethod != null && (ownerMethod.hasPrivateModifier() || ownerMethod.hasStaticModifier()))
			return null;

		// Iterate through parents and find the first method match that is inherited by the owner class.
		Iterator<InheritanceVertex> iterator = ownerVertex.allParents().iterator();
		while (iterator.hasNext()) {
			InheritanceVertex parentVertex = iterator.next();
			ClassInfo parentInfo = parentVertex.getValue();

			// If the parent class doesn't have a method with the same name/desc, skip it.
			MethodMember parentMethod = parentInfo.getDeclaredMethod(methodName, methodDesc);
			if (parentMethod == null || !isInheritedMethod(ownerInfo, ownerMethod, parentInfo, parentMethod))
				continue;

			// Otherwise, we have a method that is inherited by the owner class, so we can check for a mapping on it.
			String mapped = mappings.get(getMethodKey(parentVertex.getName(), methodName, methodDesc));
			if (mapped != null)
				return mapped;
		}

		// No mapping found in any parent class.
		return null;
	}

	/**
	 * @param ownerInfo
	 * 		Class defining the target method.
	 * @param ownerMethod
	 * 		Target method in the owner class, or {@code null} if the method is not declared in the owner class.
	 * @param parentInfo
	 * 		Class defining the parent method.
	 * @param parentMethod
	 * 		Method in the parent class with the same name/desc as the target method.
	 *
	 * @return {@code true} when the parent method is inherited/implemented by the owner class.
	 * {@code false} when the parent method cannot be inherited/implemented by the owner class due to access restrictions.
	 */
	protected static boolean isInheritedMethod(@Nonnull ClassInfo ownerInfo, @Nullable MethodMember ownerMethod,
	                                           @Nonnull ClassInfo parentInfo, @Nonnull MethodMember parentMethod) {
		// Cannot inherit private/static methods.
		if (parentMethod.hasPrivateModifier() || parentMethod.hasStaticModifier())
			return false;

		// Package-private inheritance only works if the owners are in the same package.
		String ownerPackage = ownerInfo.getPackageName();
		String parentPackage = parentInfo.getPackageName();
		if (ownerMethod == null)
			return !parentMethod.hasPackagePrivateModifier()
					|| Objects.equals(ownerPackage, parentPackage);
		return (!ownerMethod.hasPackagePrivateModifier() && !parentMethod.hasPackagePrivateModifier())
				|| Objects.equals(ownerPackage, parentPackage);
	}

	/**
	 * @param ownerVertex
	 * 		Vertex defining the explicit mapping.
	 * @param ownerInfo
	 * 		Class defining the explicit mapping method.
	 * @param ownerMethod
	 * 		Explicitly mapped method.
	 * @param familyVertex
	 * 		Candidate family vertex to apply the same mapping to.
	 * @param familyInfo
	 * 		Class defining the candidate family method.
	 * @param familyMethod
	 * 		Candidate family method.
	 *
	 * @return {@code true} when both methods belong to the same override/implementation family.
	 */
	protected static boolean isSameMethodFamily(@Nonnull InheritanceVertex ownerVertex,
	                                            @Nonnull ClassInfo ownerInfo,
	                                            @Nonnull MethodMember ownerMethod,
	                                            @Nonnull InheritanceVertex familyVertex,
	                                            @Nonnull ClassInfo familyInfo,
	                                            @Nonnull MethodMember familyMethod) {
		return isSameMethodFamily(ownerVertex, ownerInfo, ownerMethod, familyVertex, familyInfo, familyMethod, new MethodFamilyRelationCache());
	}

	/**
	 * @param ownerVertex
	 * 		Vertex defining the explicit mapping.
	 * @param ownerInfo
	 * 		Class defining the explicit mapping method.
	 * @param ownerMethod
	 * 		Explicitly mapped method.
	 * @param familyVertex
	 * 		Candidate family vertex to apply the same mapping to.
	 * @param familyInfo
	 * 		Class defining the candidate family method.
	 * @param familyMethod
	 * 		Candidate family method.
	 * @param relations Cache of ancestry/descendant relations to avoid repeated graph traversals.
	 *
	 * @return {@code true} when both methods belong to the same override/implementation family.
	 */
	private static boolean isSameMethodFamily(@Nonnull InheritanceVertex ownerVertex,
	                                         @Nonnull ClassInfo ownerInfo,
	                                         @Nonnull MethodMember ownerMethod,
	                                         @Nonnull InheritanceVertex familyVertex,
	                                         @Nonnull ClassInfo familyInfo,
	                                         @Nonnull MethodMember familyMethod,
	                                         @Nonnull MethodFamilyRelationCache relations) {
		if (ownerVertex == familyVertex)
			return true;

		// Cache the owner's ancestry and descendant graphs to avoid repeated graph traversals for every family member.
		Set<InheritanceVertex> ownerParents = relations.parents(ownerVertex);
		if (ownerParents.contains(familyVertex))
			return isInheritedMethod(ownerInfo, ownerMethod, familyInfo, familyMethod);
		Set<InheritanceVertex> ownerChildren = relations.children(ownerVertex);
		if (ownerChildren.contains(familyVertex))
			return isInheritedMethod(familyInfo, familyMethod, ownerInfo, ownerMethod);

		// Indirect family members only share a mapping if they both implement/override
		// a method declared by some common ancestor or interface.
		Iterator<InheritanceVertex> parentIterator = familyVertex.allParents().iterator();
		while (parentIterator.hasNext()) {
			InheritanceVertex commonParent = parentIterator.next();
			if (!ownerParents.contains(commonParent))
				continue;

			ClassInfo commonInfo = commonParent.getValue();
			MethodMember commonMethod = commonInfo.getDeclaredMethod(ownerMethod.getName(), ownerMethod.getDescriptor());
			if (commonMethod == null)
				continue;
			if (!isInheritedMethod(ownerInfo, ownerMethod, commonInfo, commonMethod))
				continue;
			if (!isInheritedMethod(familyInfo, familyMethod, commonInfo, commonMethod))
				continue;
			return true;
		}

		Iterator<InheritanceVertex> childIterator = familyVertex.allChildren().iterator();
		while (childIterator.hasNext()) {
			InheritanceVertex commonChild = childIterator.next();
			if (!ownerChildren.contains(commonChild))
				continue;

			ClassInfo childInfo = commonChild.getValue();
			MethodMember childMethod = childInfo.getDeclaredMethod(ownerMethod.getName(), ownerMethod.getDescriptor());
			if (!isInheritedMethod(childInfo, childMethod, ownerInfo, ownerMethod))
				continue;
			if (!isInheritedMethod(childInfo, childMethod, familyInfo, familyMethod))
				continue;
			return true;
		}

		return false;
	}

	/**
	 * @param internalName
	 * 		Some class name.
	 *
	 * @return {@code true} when the class by the given name is an inner class of another class.
	 */
	private boolean isInner(String internalName) {
		int splitIndex = internalName.lastIndexOf("$");
		// Ensure there is text before and after the split
		return splitIndex > 1 && splitIndex < internalName.length() - 1;
	}

	/**
	 * Allows the mappings to use class inheritance graphs to check for key matches where the field or method is
	 * defined in a parent class.<br>
	 * An example of this would be if you were looking to map {@link Map#size()} but the reference in the bytecode
	 * was to {@link TreeMap#size()}. If you only have the {@link Map} entry you need the type hierarchy to find that
	 * {@link TreeMap} is a child of {@link Map} and thus should <i>"inherit"</i> the mapping of {@link Map#size()}.
	 *
	 * @param inheritanceGraph
	 * 		Inheritance graph to use.
	 */
	public void enableHierarchyLookup(@Nonnull InheritanceGraph inheritanceGraph) {
		this.inheritanceGraph = inheritanceGraph;
		inheritedFieldLookupCache = null;
		inheritedMethodLookupCache = null;
	}

	/**
	 * Allows the mappings to use data from classes in the workspace to better handle some edge cases.
	 * For example, inner class name handling in {@link #getMappedClassName(String)}.
	 *
	 * @param workspace
	 * 		Workspace to pull from.
	 */
	public void enableClassLookup(@Nonnull Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * Add mapping for class name.
	 *
	 * @param originalName
	 * 		Original name.
	 * @param renamedName
	 * 		New name.
	 */
	public void addClass(@Nonnull String originalName, @Nonnull String renamedName) {
		mappings.put(getClassKey(originalName), renamedName);
	}

	/**
	 * Add mapping for field name. <br>
	 * Used when {@link #doesSupportFieldTypeDifferentiation()} is {@code true}.
	 *
	 * @param owner
	 * 		Class name defining the field.
	 * @param originalName
	 * 		Original name of the field.
	 * @param desc
	 * 		Type descriptor of the field.
	 * @param renamedName
	 * 		New name of the method.
	 */
	public void addField(@Nonnull String owner, @Nonnull String originalName, @Nonnull String desc, @Nonnull String renamedName) {
		if (doesSupportFieldTypeDifferentiation()) {
			mappings.put(getFieldKey(owner, originalName, desc), renamedName);
			inheritedFieldLookupCache = null;
		} else {
			throw new IllegalStateException("The current mapping implementation does not support " +
					"field type differentiation");
		}
	}

	/**
	 * Add mapping for field name.<br>
	 * Used when {@link #doesSupportFieldTypeDifferentiation()} is {@code false}.
	 *
	 * @param owner
	 * 		Class name defining the field.
	 * @param originalName
	 * 		Original name of the field.
	 * @param renamedName
	 * 		New name of the field.
	 */
	public void addField(@Nonnull String owner, @Nonnull String originalName, @Nonnull String renamedName) {
		if (doesSupportFieldTypeDifferentiation()) {
			throw new IllegalStateException("The current mapping implementation requires " +
					"specifying field descriptors");
		} else {
			mappings.put(getFieldKey(owner, originalName, null), renamedName);
			inheritedFieldLookupCache = null;
		}
	}

	/**
	 * Add mapping for method name.
	 *
	 * @param owner
	 * 		Class name defining the method.
	 * @param originalName
	 * 		Original name of the method.
	 * @param desc
	 * 		Type descriptor of the method.
	 * @param renamedName
	 * 		New name of the method.
	 */
	public void addMethod(@Nonnull String owner, @Nonnull String originalName, @Nonnull String desc, @Nonnull String renamedName) {
		mappings.put(getMethodKey(owner, originalName, desc), renamedName);
		inheritedMethodLookupCache = null;
	}

	/**
	 * Add mapping for variable name.
	 *
	 * @param className
	 * 		Class name defining the method.
	 * @param methodName
	 * 		Method name.
	 * @param methodDesc
	 * 		Method descriptor.
	 * @param originalName
	 * 		Variable original name.
	 * @param desc
	 * 		Variable descriptor.
	 * @param index
	 * 		Variable index.
	 * @param renamedName
	 * 		New name of the variable.
	 */
	public void addVariable(@Nonnull String className, @Nonnull String methodName, @Nonnull String methodDesc,
	                        @Nonnull String originalName, @Nullable String desc, int index, @Nonnull String renamedName) {
		MappingKey key = getVariableKey(className, methodName, methodDesc, originalName, desc, index);
		mappings.put(key, renamedName);
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Key format for class.
	 */
	@Nonnull
	protected MappingKey getClassKey(@Nonnull String name) {
		return new ClassMappingKey(name);
	}

	/**
	 * @param ownerName
	 * 		Class defining the field.
	 * @param fieldName
	 * 		Name of field.
	 * @param fieldDesc
	 * 		Type descriptor of field.
	 *
	 * @return Key format for field.
	 */
	@Nonnull
	protected MappingKey getFieldKey(@Nonnull String ownerName, @Nonnull String fieldName, @Nullable String fieldDesc) {
		return new FieldMappingKey(ownerName, fieldName, supportFieldTypeDifferentiation ? fieldDesc : null);
	}

	/**
	 * @param ownerName
	 * 		Class defining the method.
	 * @param methodName
	 * 		Name of method.
	 * @param methodDesc
	 * 		Type descriptor of method.
	 *
	 * @return Key format for method.
	 */
	@Nonnull
	protected MappingKey getMethodKey(@Nonnull String ownerName, @Nonnull String methodName, @Nonnull String methodDesc) {
		return new MethodMappingKey(ownerName, methodName, methodDesc);
	}

	/**
	 * @param className
	 * 		Class defining the method.
	 * @param methodName
	 * 		Name of method.
	 * @param methodDesc
	 * 		Type descriptor of method.
	 * @param name
	 * 		Name of variable.
	 * @param desc
	 * 		Type descriptor of variable.
	 * @param index
	 * 		Local variable table index.
	 *
	 * @return Key format for variable.
	 */
	@Nonnull
	protected MappingKey getVariableKey(@Nonnull String className, @Nonnull String methodName, @Nonnull String methodDesc,
	                                    @Nullable String name, @Nullable String desc, int index) {
		return new VariableMappingKey(className, methodName, methodDesc, name, desc, index);
	}

	/**
	 * Build the mapping conflict exception. Occurs when you have a mapping setup like:
	 * <pre>
	 * {@code
	 * Shape.area() -> foo
	 * Circle.area() -> bar
	 * }</pre>
	 * If {@code Circle} implements {@code Shape}, we cannot have two names for the same 'family'.
	 *
	 * @param key
	 * 		Method info.
	 * @param affectedOwner
	 * 		Class that is affected by the conflict.
	 * @param previous
	 * 		Existing mapping that is causing the conflict.
	 * @param current
	 * 		New method mapping that is causing the conflict.
	 *
	 * @return Exception detailing the method mapping conflict.
	 */
	@Nonnull
	private static IllegalStateException newMethodConflict(@Nonnull MethodMappingKey key, @Nonnull String affectedOwner,
	                                                       @Nonnull MethodMapping previous, @Nonnull MethodMapping current) {
		return new IllegalStateException("Conflicting method mapping for family '" +
				key.getName() + key.getDesc() + "' affecting '" + affectedOwner + "': '" +
				previous.getOwnerName() + "' -> '" + previous.getNewName() + "' conflicts with '" +
				current.getOwnerName() + "' -> '" + current.getNewName() + "'");
	}

	/**
	 * Method family entry.
	 *
	 * @param vertex
	 * 		Inheritance vertex of the method.
	 * @param ownerInfo
	 * 		Class info of the method owner.
	 * @param method
	 * 		Method member.
	 */
	private record MethodFamilyEntry(@Nonnull InheritanceVertex vertex, @Nonnull ClassInfo ownerInfo, @Nonnull MethodMember method) {}

	/**
	 * Lazily caches ancestry and descendant graphs for method family lookups to avoid repeated graph traversals.
	 */
	private static final class MethodFamilyRelationCache {
		private final Map<InheritanceVertex, Set<InheritanceVertex>> parents = new IdentityHashMap<>();
		private final Map<InheritanceVertex, Set<InheritanceVertex>> children = new IdentityHashMap<>();

		@Nonnull
		private Set<InheritanceVertex> parents(@Nonnull InheritanceVertex vertex) {
			Set<InheritanceVertex> result = parents.get(vertex);
			if (result == null) {
				result = vertex.getAllParents();
				parents.put(vertex, result);
			}
			return result;
		}

		@Nonnull
		private Set<InheritanceVertex> children(@Nonnull InheritanceVertex vertex) {
			Set<InheritanceVertex> result = children.get(vertex);
			if (result == null) {
				result = vertex.getAllChildren();
				children.put(vertex, result);
			}
			return result;
		}
	}

	/**
	 * Caches results of looking up a field/method in parent classes to avoid repeated lookups.
	 */
	private static final class HierarchyLookupCache {
		private final Map<MappingKey, Object> results = new ConcurrentHashMap<>();
		private final AtomicInteger entryCount = new AtomicInteger();

		@Nullable
		private Object get(@Nonnull MappingKey key) {
			return results.get(key);
		}

		@Nonnull
		private Object putIfSpace(@Nonnull MappingKey key, @Nonnull Object value) {
			// Reserve a slot before insertion so concurrent misses cannot exceed the cache limit.
			int count = entryCount.get();
			while (count < MAX_HIERARCHY_LOOKUP_CACHE_ENTRIES) {
				if (entryCount.compareAndSet(count, count + 1)) {
					Object previous = results.putIfAbsent(key, value);
					if (previous == null)
						return value;
					entryCount.decrementAndGet();
					return previous;
				}
				count = entryCount.get();
			}
			return value;
		}
	}
}
