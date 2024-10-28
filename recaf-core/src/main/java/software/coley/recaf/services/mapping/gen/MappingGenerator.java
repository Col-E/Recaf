package software.coley.recaf.services.mapping.gen;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.MappingsAdapter;
import software.coley.recaf.services.mapping.gen.filter.ExcludeEnumMethodsFilter;
import software.coley.recaf.services.mapping.gen.filter.NameGeneratorFilter;
import software.coley.recaf.services.mapping.gen.naming.NameGenerator;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Mapping generator.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class MappingGenerator implements Service {
	public static final String SERVICE_ID = "mapping-generator";
	private final MappingGeneratorConfig config;

	@Inject
	public MappingGenerator(@Nonnull MappingGeneratorConfig config) {
		this.config = config;
	}

	/**
	 * @param workspace
	 * 		Workspace to pull class information from.
	 * 		Can be {@code null} but some assumptions will be made about inner-class names.
	 * @param resource
	 * 		Resource to generate mappings for.
	 * @param inheritanceGraph
	 * 		Inheritance graph to determine class hierarchies.
	 * @param generator
	 * 		Name generation implementation.
	 * @param filter
	 * 		Name generation filter, used to limit which classes and members get renamed.
	 *
	 * @return Newly generated mappings.
	 */
	@Nonnull
	public Mappings generate(@Nullable Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull InheritanceGraph inheritanceGraph,
	                         @Nonnull NameGenerator generator,
	                         @Nullable NameGeneratorFilter filter) {
		// Adapt filter to handle baseline cases.
		filter = new ExcludeEnumMethodsFilter(filter);

		// Setup adapter to store our mappings in.
		MappingsAdapter mappings = new MappingsAdapter(true, true);
		mappings.enableHierarchyLookup(inheritanceGraph);
		if (workspace != null)
			mappings.enableClassLookup(workspace);
		SortedMap<String, ClassInfo> classMap = new TreeMap<>();
		resource.versionedJvmClassBundleStream()
				.flatMap(Bundle::stream)
				.forEach(c -> classMap.put(c.getName(), c));
		classMap.putAll(resource.getJvmClassBundle());

		// Pull a class, create mappings for its inheritance family, then remove those classes from the map.
		// When the map is empty everything has been run through the mapping generation process.
		while (!classMap.isEmpty()) {
			// Get family from the class.
			String className = classMap.firstKey();
			Set<InheritanceVertex> family = inheritanceGraph.getVertexFamily(className, false);

			// Create mappings for the family
			generateFamilyMappings(mappings, family, generator, filter);

			// Remove all family members from the class map.
			if (family.isEmpty())
				classMap.remove(className);
			else
				family.forEach(vertex -> classMap.remove(vertex.getName()));
		}
		return mappings;
	}

	private void generateFamilyMappings(@Nonnull MappingsAdapter mappings, @Nonnull Set<InheritanceVertex> family,
	                                    @Nonnull NameGenerator generator, @Nonnull NameGeneratorFilter filter) {
		// Collect the members in the family that are inheritable, and methods that are library implementations.
		// We want this information so that for these members we give them a single name throughout the family.
		//  - Methods can be indirectly linked by two interfaces describing the same signature,
		//    and a child type implementing both types. So we have to be strict with naming with cases like this.
		//  - Fields do not have such a concern, but can still be accessed by child type owners.
		Set<MemberKey> inheritableFields = new HashSet<>();
		Set<MemberKey> inheritableMethods = new HashSet<>();
		Set<MemberKey> libraryMethods = new HashSet<>();
		family.forEach(vertex -> {
			// Skip module-info classes
			if (vertex.isModule())
				return;

			// Record fields/methods, skipping private items since they cannot span the hierarchy.
			for (FieldMember field : vertex.getValue().getFields()) {
				if (field.hasPrivateModifier())
					continue;
				inheritableFields.add(MemberKey.of(field));
			}
			for (MethodMember method : vertex.getValue().getMethods()) {
				if (method.hasPrivateModifier())
					continue;
				MemberKey key = MemberKey.of(method);
				inheritableMethods.add(key);

				// Need to track which methods we cannot remap due to them being overrides of libraries
				// rather than being declared solely in our resource.
				if (vertex.isLibraryMethod(method.getName(), method.getDescriptor()))
					libraryMethods.add(key);
			}
		});
		// Create mappings for members.
		family.forEach(vertex -> {
			// Skip libraries in the family.
			if (vertex.isLibraryVertex())
				return;

			// Skip module-info classes
			if (vertex.isModule())
				return;

			ClassInfo owner = vertex.getValue();
			String ownerName = owner.getName();
			for (FieldMember field : owner.getFields()) {
				String fieldName = field.getName();
				String fieldDesc = field.getDescriptor();

				// Skip if filtered.
				if (!filter.shouldMapField(owner, field))
					continue;

				// Skip if already mapped.
				if (mappings.getMappedFieldName(ownerName, fieldName, fieldDesc) != null)
					continue;

				// Create mapped name and record into mappings.
				MemberKey key = MemberKey.of(field);
				String mappedFieldName = generator.mapField(owner, field);
				if (inheritableFields.contains(key)) {
					// Field is 'inheritable' meaning it needs to have a consistent name
					// for all children and parents of this vertex.
					Set<InheritanceVertex> targetFamilyMembers = new HashSet<>();
					targetFamilyMembers.add(vertex);
					targetFamilyMembers.addAll(vertex.getAllChildren());
					targetFamilyMembers.addAll(vertex.getAllParents());
					targetFamilyMembers.forEach(immediateTreeVertex -> {
						if (immediateTreeVertex.hasField(fieldName, fieldDesc)) {
							String treeOwner = immediateTreeVertex.getName();
							mappings.addField(treeOwner, fieldName, fieldDesc, mappedFieldName);
						}
					});
				} else {
					// Not 'inheritable' so an independent mapping is all we need.
					mappings.addField(ownerName, fieldName, fieldDesc, mappedFieldName);
				}
			}
			for (MethodMember method : owner.getMethods()) {
				String methodName = method.getName();
				String methodDesc = method.getDescriptor();

				// Skip if reserved method name.
				if (!methodName.isEmpty() && methodName.charAt(0) == '<')
					continue;

				// Skip if filtered.
				if (!filter.shouldMapMethod(owner, method))
					continue;

				// Skip if method is a library method, or is already mapped.
				MemberKey key = MemberKey.of(method);
				if (libraryMethods.contains(key) || mappings.getMappedMethodName(ownerName, methodName, methodDesc) != null)
					continue;

				// Create variable mappings
				for (LocalVariable variable : method.getLocalVariables()) {
					String variableName = variable.getName();

					// Do not rename 'this' local variable... Unless its not "this" then force it to be "this"
					if (variable.getIndex() == 0 && !method.hasStaticModifier()) {
						if (!"this".equals(variableName))
							mappings.addVariable(ownerName, methodName, methodDesc, variableName, variable.getDescriptor(), variable.getIndex(), "this");
						continue;
					}

					if (filter.shouldMapLocalVariable(owner, method, variable)) {
						String mappedVariableName = generator.mapVariable(owner, method, variable);
						if (!mappedVariableName.equals(variableName)) {
							mappings.addVariable(ownerName, methodName, methodDesc, variableName, variable.getDescriptor(), variable.getIndex(), mappedVariableName);
						}
					}
				}

				// Create mapped name and record into mappings.
				String mappedMethodName = generator.mapMethod(owner, method);

				// Skip if the name generator yields the same name back.
				if (methodName.equals(mappedMethodName))
					continue;

				if (inheritableMethods.contains(key)) {
					// Method is 'inheritable' meaning it needs to have a consistent name for the entire family.
					// But if one of the members of the family is filtered, then we cannot map anything.
					boolean shouldMapFamily = true;
					List<Runnable> pendingMapAdditions = new ArrayList<>();
					for (InheritanceVertex familyVertex : family) {
						if (familyVertex.hasMethod(methodName, methodDesc)) {
							if (filter.shouldMapMethod(familyVertex.getValue(), method)) {
								pendingMapAdditions.add(() ->
										mappings.addMethod(familyVertex.getName(), methodName, methodDesc, mappedMethodName));
							} else {
								shouldMapFamily = false;
								pendingMapAdditions.clear();
								break;
							}
						}
					}

					// Nothing in the family was filtered, we can add the method mappings.
					if (shouldMapFamily)
						pendingMapAdditions.forEach(Runnable::run);
				} else {
					// Not 'inheritable' so an independent mapping is all we need.
					mappings.addMethod(ownerName, methodName, methodDesc, mappedMethodName);
				}
			}
		});
		// Create mappings for classes.
		family.forEach(vertex -> {
			// Skip libraries in the family.
			if (vertex.isLibraryVertex())
				return;
			// Skip module-info classes
			if (vertex.isModule())
				return;

			// Skip if filtered.
			ClassInfo classInfo = vertex.getValue();
			if (!filter.shouldMapClass(classInfo))
				return;

			// Add mapping.
			String name = vertex.getName();
			String mapped = generator.mapClass(classInfo);
			mappings.addClass(name, mapped);
		});
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public MappingGeneratorConfig getServiceConfig() {
		return config;
	}

	/**
	 * Local record to use as set entries that are simpler than {@link ClassMember} implementations.
	 * <p/>
	 * Most importantly the {@link Object#hashCode()} of this type is based only on the name and descriptor.
	 * This ensures additional data like local variable or generic signature data doesn't interfere with operations
	 * such as {@link #generateFamilyMappings(MappingsAdapter, Set, NameGenerator, NameGeneratorFilter)}.
	 *
	 * @param name
	 * 		Field/method name.
	 * @param descriptor
	 * 		Field/method descriptor.
	 */
	private record MemberKey(@Nonnull String name, @Nonnull String descriptor) {
		@Nonnull
		static MemberKey of(@Nonnull ClassMember member) {
			return new MemberKey(member.getName(), member.getDescriptor());
		}
	}
}
