package me.coley.recaf.mapping;

import me.coley.recaf.graph.InheritanceGraph;
import me.coley.recaf.graph.InheritanceVertex;
import me.coley.recaf.mapping.data.ClassMapping;
import me.coley.recaf.mapping.data.FieldMapping;
import me.coley.recaf.mapping.data.MethodMapping;
import me.coley.recaf.mapping.data.VariableMapping;
import me.coley.recaf.mapping.impl.IntermediateMappings;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Basic groundwork for any mapping implementation. <br>
 * <br>
 * Mapping capabilities are defined in the constructor:
 * <ul>
 *     <li>{@link #doesSupportFieldTypeDifferentiation()}</li>
 *     <li>{@link #doesSupportVariableTypeDifferentiation()}</li>
 * </ul>
 * Allows hierarchy look-ups <i>(More info given at: {@link #enableHierarchyLookup(InheritanceGraph)}</i><br>
 * Handles inner class relations in class look-ups.
 *
 * @author Matt Coley
 */
public class MappingsAdapter implements Mappings {
	private static final String SPLIT_PATTERN = "\t";
	private static final String FIELD_KEY_FMT = "%s\t%s";
	private static final String FIELD_KEY_TYPED_FMT = "%s\t%s\t%s";
	private static final String METHOD_KEY_FMT = "%s\t%s\t%s";
	private static final String VAR_KEY_FMT = "%s\t%s\t%s\t%s";
	private static final String VAR_KEY_TYPED_FMT = "%s\t%s\t%s\t%s\t%s";
	private final Map<String, String> mappings = new TreeMap<>();
	private final String implementationName;
	private final boolean supportFieldTypeDifferentiation;
	private final boolean supportVariableTypeDifferentiation;
	private InheritanceGraph graph;

	/**
	 * @param implementationName
	 * 		Name of the mapping format implementation.
	 * @param supportFieldTypeDifferentiation
	 *        {@code true} if the mapping format implementation includes type descriptors in field mappings.
	 * @param supportVariableTypeDifferentiation
	 *        {@code true} if the mapping format implementation includes type descriptors in variable mappings.
	 */
	public MappingsAdapter(String implementationName, boolean supportFieldTypeDifferentiation,
						   boolean supportVariableTypeDifferentiation) {
		this.implementationName = implementationName;
		this.supportFieldTypeDifferentiation = supportFieldTypeDifferentiation;
		this.supportVariableTypeDifferentiation = supportVariableTypeDifferentiation;
	}

	@Override
	public String getMappedClassName(String internalName) {
		String mapped = mappings.getOrDefault(internalName, null);
		if (mapped == null && isInner(internalName)) {
			// TODO: Similar to providing the 'graph' we can provide a workspace that will let us access the class's
			//  actual attributes. We can check for inner classes that way in case an obfuscated sample disregards
			//  the standard inner class naming convention.
			int split = internalName.lastIndexOf("$");
			String inner = internalName.substring(split + 1);
			String outer = internalName.substring(0, split);
			String outerMapped = getMappedClassName(outer);
			if (outerMapped != null) {
				mapped = outerMapped + "$" + inner;
			}
		}
		return mapped;
	}

	@Override
	public String getMappedFieldName(String ownerName, String fieldName, String fieldDesc) {
		String key = getFieldKey(ownerName, fieldName, fieldDesc);
		String mapped = mappings.getOrDefault(key, null);
		if (mapped == null && graph != null) {
			mapped = findInParent(ownerName, parent -> getFieldKey(parent, fieldName, fieldDesc));
		}
		return mapped;
	}

	@Override
	public String getMappedMethodName(String ownerName, String methodName, String methodDesc) {
		String key = getMethodKey(ownerName, methodName, methodDesc);
		String mapped = mappings.getOrDefault(key, null);
		if (mapped == null && graph != null) {
			mapped = findInParent(ownerName, parent -> getMethodKey(parent, methodName, methodDesc));
		}
		return mapped;
	}

	@Override
	public String getMappedVariableName(String className, String methodName, String methodDesc,
										String name, String desc, int index) {
		String key = getVariableKey(className, methodName, methodDesc, name, desc, index);
		return mappings.getOrDefault(key, null);
	}

	@Override
	public String implementationName() {
		return implementationName;
	}

	@Override
	public void parse(String mappingsText) {
		// No op
	}

	@Override
	public boolean supportsExportIntermediate() {
		return true;
	}

	@Override
	public IntermediateMappings exportIntermediate() {
		IntermediateMappings intermediate = new IntermediateMappings();
		for (Map.Entry<String, String> entry : mappings.entrySet()) {
			String key = entry.getKey();
			String newName = entry.getValue();
			if (!key.contains(SPLIT_PATTERN)) {
				// Must be a class
				intermediate.addClass(key, newName);
			} else if (key.contains("(")) {
				String[] split = key.split(SPLIT_PATTERN);
				// Must be a method or variable
				String oldOwner = split[0];
				String oldName = split[1];
				String oldDesc = split[2];
				if (split.length >= 4) {
					// variable
				} else {
					// method
					intermediate.addMethod(oldOwner, oldDesc, oldName, newName);
				}
			} else {
				// Must be a field
				String[] split = key.split(SPLIT_PATTERN);
				String oldOwner = split[0];
				String oldName = split[1];
				if (split.length >= 3) {
					// Field with type
					String oldDesc = split[2];
					intermediate.addField(oldOwner, oldDesc, oldName, newName);
				} else {
					// Field
					intermediate.addField(oldOwner, null, oldName, newName);
				}
			}
		}
		return intermediate;
	}

	@Override
	public void importIntermediate(IntermediateMappings mappings) {
		for (ClassMapping classMapping : mappings.getClasses().values()) {
			String oldClassName = classMapping.getOldName();
			addClass(oldClassName, classMapping.getNewName());
			for (FieldMapping fieldMapping : mappings.getClassFieldMappings(oldClassName)) {
				if (doesSupportFieldTypeDifferentiation()) {
					addField(fieldMapping.getOwnerName(), fieldMapping.getOldName(),
							fieldMapping.getDesc(), fieldMapping.getNewName());
				} else {
					addField(fieldMapping.getOwnerName(), fieldMapping.getOldName(),
							fieldMapping.getNewName());
				}
			}
			for (MethodMapping methodMapping : mappings.getClassMethodMappings(oldClassName)) {
				String oldMethodName = methodMapping.getOldName();
				String oldMethodDesc = methodMapping.getDesc();
				addMethod(methodMapping.getOwnerName(), oldMethodName,
						oldMethodDesc, methodMapping.getNewName());
				for (VariableMapping variableMapping :
						mappings.getMethodVariableMappings(oldClassName, oldMethodName, oldMethodDesc)) {
					addVariable(oldClassName, oldMethodName, oldMethodDesc,
							variableMapping.getOldName(), variableMapping.getDesc(), variableMapping.getIndex(),
							variableMapping.getNewName());
				}
			}
		}
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
	private String findInParent(String owner, Function<String, String> lookup) {
		return graph.getVertex(owner).getParents().stream()
				.map(InheritanceVertex::getName)
				.map(lookup)
				.filter(Objects::nonNull)
				.findFirst().orElse(null);
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
	 * @param graph
	 * 		Inheritance graph to use.
	 */
	public void enableHierarchyLookup(InheritanceGraph graph) {
		this.graph = graph;
	}

	/**
	 * Some mapping formats do not include field types since name overloading is illegal at the source level of Java.
	 * Its valid in the bytecode but the mapping omits this info since it isn't necessary information for mapping
	 * that does not support name overloading.
	 *
	 * @return {@code true} when field mappings include the type descriptor in their lookup information.
	 */
	public boolean doesSupportFieldTypeDifferentiation() {
		return supportFieldTypeDifferentiation;
	}

	/**
	 * Some mapping forats do not include variable types since name overloading is illegal at the source level of Java.
	 * Variable names are not used by the JVM at all so their names can be anything at the bytecode level. So including
	 * the type makes it easier to reverse mappings.
	 *
	 * @return {@code true} when variable mappings include the type descriptor in their lookup information.
	 */
	public boolean doesSupportVariableTypeDifferentiation() {
		return supportVariableTypeDifferentiation;
	}

	/**
	 * Add mapping for class name.
	 *
	 * @param originalName
	 * 		Original name.
	 * @param renamedName
	 * 		New name.
	 */
	public void addClass(String originalName, String renamedName) {
		mappings.put(originalName, renamedName);
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
	public void addField(String owner, String originalName, String desc, String renamedName) {
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
	public void addField(String owner, String originalName, String renamedName) {
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
	public void addMethod(String owner, String originalName, String desc, String renamedName) {
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
	public void addVariable(String className, String methodName, String methodDesc,
							String originalName, String desc, int index, String renamedName) {
		String key = getVariableKey(className, methodName, methodDesc, originalName, desc, index);
		mappings.put(key, renamedName);
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
	protected String getFieldKey(String ownerName, String fieldName, String fieldDesc) {
		if (supportFieldTypeDifferentiation) {
			return String.format(FIELD_KEY_TYPED_FMT, ownerName, fieldName, fieldDesc);
		} else {
			return String.format(FIELD_KEY_FMT, ownerName, fieldName);
		}
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
	protected String getMethodKey(String ownerName, String methodName, String methodDesc) {
		return String.format(METHOD_KEY_FMT, ownerName, methodName, methodDesc);
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
	protected String getVariableKey(String className, String methodName, String methodDesc,
									String name, String desc, int index) {
		if (supportVariableTypeDifferentiation) {
			return String.format(VAR_KEY_TYPED_FMT, className, methodName, methodDesc, name, desc);
		} else {
			return String.format(VAR_KEY_FMT, className, methodName, methodDesc, name);
		}
	}
}
