package me.coley.recaf.mapping.gen;

import me.coley.recaf.Controller;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.graph.InheritanceGraph;
import me.coley.recaf.graph.InheritanceVertex;
import me.coley.recaf.mapping.Mappings;
import me.coley.recaf.mapping.MappingsAdapter;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.workspace.resource.Resource;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Mapping generator.
 *
 * @author Matt Coley
 */
public class MappingGenerator {
	private final Resource resource;
	private final InheritanceGraph inheritanceGraph;
	private NameGenerator nameGenerator;
	private NameGeneratorFilter filter;

	/**
	 * @param controller
	 * 		Controller to pull primary resource and inheritance graph from.
	 */
	public MappingGenerator(Controller controller) {
		this(controller.getWorkspace().getResources().getPrimary(),
				controller.getServices().getInheritanceGraph());
	}

	/**
	 * @param resource
	 * 		Resource to generate mappings for.
	 * @param inheritanceGraph
	 * 		Inheritance graph to determine class hierarchies.
	 */
	public MappingGenerator(Resource resource, InheritanceGraph inheritanceGraph) {
		this.resource = requireNonNull(resource, "Must supply a resource to generate mappings for");
		this.inheritanceGraph = requireNonNull(inheritanceGraph, "Must supply an inheritance graph for the resource");
	}

	/**
	 * @return Newly generated mappings.
	 */
	public Mappings generate() {
		MappingsAdapter mappings = new MappingsAdapter("MAP-GEN", true, true);
		mappings.enableHierarchyLookup(inheritanceGraph);
		SortedMap<String, ClassInfo> classMap = new TreeMap<>(resource.getClasses());
		// Pull a class, create mappings for its inheritance family, then remove those classes from the map.
		// When the map is empty everything has been run through the mapping generation process.
		while (!classMap.isEmpty()) {
			// Get family from the class.
			String className = classMap.firstKey();
			Set<InheritanceVertex> family = inheritanceGraph.getVertexFamily(className);
			// Create mappings for the family
			generateFamilyMappings(mappings, family);
			// Remove all family members from the class map.
			family.forEach(vertex -> classMap.remove(vertex.getName()));
		}
		return mappings;
	}

	private void generateFamilyMappings(MappingsAdapter mappings, Set<InheritanceVertex> family) {
		// Collect the members in the family that are inheritable, and methods that are library implementations.
		// We want this information so that for these members we give them a single name throughout the family.
		//  - Methods can be indirectly linked by two interfaces describing the same signature,
		//    and a child type implementing both types. So we have to be strict with naming with cases like this.
		//  - Fields do not have such a concern, but can still be accessed by child type owners.
		Set<FieldInfo> inheritableFields = new HashSet<>();
		Set<MethodInfo> inheritableMethods = new HashSet<>();
		Set<MethodInfo> libraryMethods = new HashSet<>();
		family.forEach(vertex -> {
			// Record fields/methods.
			for (FieldInfo field : vertex.getValue().getFields()) {
				if (AccessFlag.isPrivate(field.getAccess()))
					continue;
				inheritableFields.add(field);
			}
			for (MethodInfo method : vertex.getValue().getMethods()) {
				if (AccessFlag.isPrivate(method.getAccess()))
					continue;
				inheritableMethods.add(method);
				// Need to track which methods we cannot remap due to them being overrides of libraries
				// rather than being declared solely in our resource.
				if (vertex.isLibraryMethod(method.getName(), method.getDescriptor()))
					libraryMethods.add(method);
			}
		});
		// Create mappings for members.
		family.forEach(vertex -> {
			// Skip libraries in the family.
			if (vertex.isLibraryVertex())
				return;
			CommonClassInfo owner = vertex.getValue();
			String ownerName = owner.getName();
			for (FieldInfo field : owner.getFields()) {
				String fieldName = field.getName();
				String fieldDesc = field.getDescriptor();
				// Skip if filtered.
				if (filter != null && !filter.shouldMapField(owner, field))
					continue;
				// Skip if already mapped.
				if (mappings.getMappedFieldName(ownerName, fieldName, fieldDesc) != null)
					continue;
				// Create mapped name and record into mappings.
				String mappedFieldName = nameGenerator.mapField(owner, field);
				if (inheritableFields.contains(field)) {
					// Field is 'inheritable' meaning it needs to have a consistent name
					// for all children and parents of this vertex.
					Set<InheritanceVertex> targetFamilyMembers = new HashSet<>();
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
			for (MethodInfo method : owner.getMethods()) {
				String methodName = method.getName();
				String methodDesc = method.getDescriptor();
				// Skip if reserved method name.
				if (methodName.charAt(0) == '<')
					continue;
				// Skip if filtered.
				if (filter != null && !filter.shouldMapMethod(owner, method))
					continue;
				// Skip if method is a library method, or is already mapped.
				if (libraryMethods.contains(method) || mappings.getMappedMethodName(ownerName, methodName, methodDesc) != null)
					continue;
				// Create mapped name and record into mappings.
				String mappedMethodName = nameGenerator.mapMethod(owner, method);
				// Skip if the name generator yields the same name back.
				if (methodName.equals(mappedMethodName))
					continue;
				if (inheritableMethods.contains(method)) {
					// Method is 'inheritable' meaning it needs to have a consistent name for the entire family.
					// But if one of the members of the family is filtered, then we cannot map anything.
					boolean shouldMapFamily = true;
					List<Runnable> pendingMapAdditions = new ArrayList<>();
					for (InheritanceVertex familyVertex : family) {
						if (familyVertex.hasMethod(methodName, methodDesc)) {
							if (filter == null || filter.shouldMapMethod(familyVertex.getValue(), method)) {
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
			// Skip if filtered.
			CommonClassInfo classInfo = vertex.getValue();
			if (filter != null && !filter.shouldMapClass(classInfo))
				return;
			// Add mapping.
			String name = vertex.getName();
			String mapped = nameGenerator.mapClass(classInfo);
			mappings.addClass(name, mapped);
		});
	}

	/**
	 * @return Resource to generate mappings for.
	 */
	public Resource getResource() {
		return resource;
	}

	/**
	 * @return Name generation implementation.
	 */
	public NameGenerator getNameGenerator() {
		return nameGenerator;
	}

	/**
	 * @param nameGenerator
	 * 		New name generation implementation.
	 */
	public void setNameGenerator(NameGenerator nameGenerator) {
		this.nameGenerator = nameGenerator;
	}

	/**
	 * @return Name generation filter, used to limit which classes and members get renamed.
	 */
	public NameGeneratorFilter getFilter() {
		return filter;
	}

	/**
	 * @param filter
	 * 		New name generation filter, used to limit which classes and members get renamed.
	 */
	public void setFilter(NameGeneratorFilter filter) {
		this.filter = filter;
	}
}
