package software.coley.recaf.services.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.services.mapping.data.*;
import software.coley.recaf.workspace.model.Workspace;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * A {@link Mappings} implementation with a number of additional operations to support usage beyond basic mapping info storage.
 * <b>Enhancements</b>
 * <ol>
 * <li>Import mapping entries from a {@link IntermediateMappings} instance.</li>
 * <li>Enhance field/method lookups with inheritance info from {@link InheritanceGraph}, see {@link #enableHierarchyLookup(InheritanceGraph)}.</li>
 * <li>Enhance inner/outer class mapping edge cases via {@link #enableClassLookup(Workspace)}.</li>
 * <li>Adapt keys in cases where fields/vars do not have type info associated with them <i>(for formats that suck)</i>.</li>
 * </ol>
 *
 * @author Matt Coley
 */
public class MappingsAdapter implements Mappings {
	private final Map<MappingKey, String> mappings = new HashMap<>();
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
						addField(fieldMapping.getOwnerName(), oldName,
								fieldMapping.getDesc(), newName);
					} else {
						addField(fieldMapping.getOwnerName(), oldName,
								newName);
					}
				}
			}
			for (MethodMapping methodMapping : mappings.getClassMethodMappings(className)) {
				String oldMethodName = methodMapping.getOldName();
				String oldMethodDesc = methodMapping.getDesc();
				String newMethodName = methodMapping.getNewName();
				if (!oldMethodName.equals(newMethodName))
					addMethod(methodMapping.getOwnerName(), oldMethodName,
							oldMethodDesc, newMethodName);
				for (VariableMapping variableMapping :
						mappings.getMethodVariableMappings(className, oldMethodName, oldMethodDesc)) {
					addVariable(className, oldMethodName, oldMethodDesc,
							variableMapping.getOldName(), variableMapping.getDesc(), variableMapping.getIndex(),
							variableMapping.getNewName());
				}
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
						if (outerMapped != null) {
							mapped = outerMapped + "$" + inner;
						}
					}
				}
			} else if (isInner(internalName)) {
				// We don't have a workspace, so the best we can do is assume standard 'Outer$Inner' conventions.
				int split = internalName.lastIndexOf("$");
				String inner = internalName.substring(split + 1);
				String outer = internalName.substring(0, split);
				String outerMapped = getMappedClassName(outer);
				if (outerMapped != null) {
					mapped = outerMapped + "$" + inner;
				}
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
			mapped = findInParent(ownerName, parent -> getFieldKey(parent, fieldName, fieldDesc));
		}
		return mapped;
	}

	@Nullable
	@Override
	public String getMappedMethodName(@Nonnull String ownerName, @Nonnull String methodName, @Nonnull String methodDesc) {
		MappingKey key = getMethodKey(ownerName, methodName, methodDesc);
		String mapped = mappings.get(key);
		if (mapped == null && inheritanceGraph != null) {
			mapped = findInParent(ownerName, parent -> getMethodKey(parent, methodName, methodDesc));
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
			if (key instanceof ClassMappingKey) {
				intermediate.addClass(((ClassMappingKey) key).getName(), newName);
			} else if (key instanceof MethodMappingKey mk) {
				intermediate.addMethod(mk.getOwner(), mk.getDesc(), mk.getName(), newName);
			} else if (key instanceof FieldMappingKey fk) {
				String oldOwner = fk.getOwner();
				String oldName = fk.getName();
				String oldDesc = fk.getDesc();
				intermediate.addField(oldOwner, oldDesc, oldName, newName);
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
	private String findInParent(String owner, Function<String, ? extends MappingKey> lookup) {
		InheritanceVertex vertex = inheritanceGraph.getVertex(owner);
		if (vertex == null)
			return null;
		Iterator<InheritanceVertex> iterator = vertex.allParents().iterator();
		while (iterator.hasNext()) {
			vertex = iterator.next();
			MappingKey key = lookup.apply(vertex.getName());
			String result = mappings.get(key);
			if (result != null) {
				return result;
			}
		}
		return null;
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
		return new VariableMappingKey(className, methodName, methodDesc, name, desc);
	}
}
