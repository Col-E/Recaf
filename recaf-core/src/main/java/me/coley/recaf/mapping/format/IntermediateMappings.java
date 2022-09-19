package me.coley.recaf.mapping.format;

import me.coley.recaf.mapping.Mappings;
import me.coley.recaf.mapping.data.ClassMapping;
import me.coley.recaf.mapping.data.FieldMapping;
import me.coley.recaf.mapping.data.MethodMapping;
import me.coley.recaf.mapping.data.VariableMapping;

import java.util.*;

/**
 * Collection of object representations of mappings. Useful as an intermediate between multiple
 * types of {@link me.coley.recaf.mapping.Mappings}.
 *
 * @author Matt Coley
 */
public class IntermediateMappings implements Mappings {
	protected final Map<String, ClassMapping> classes = new HashMap<>();
	protected final Map<String, List<FieldMapping>> fields = new HashMap<>();
	protected final Map<String, List<MethodMapping>> methods = new HashMap<>();
	protected final Map<String, List<VariableMapping>> variables = new HashMap<>();

	/**
	 * @param oldName
	 * 		Pre-mapping name.
	 * @param newName
	 * 		Post-mapping name.
	 */
	public void addClass(String oldName, String newName) {
		classes.put(oldName, new ClassMapping(oldName, newName));
	}

	/**
	 * @param ownerName
	 * 		Name of class defining the field.
	 * @param desc
	 * 		Descriptor type of the field.
	 * @param oldName
	 * 		Pre-mapping field name.
	 * @param newName
	 * 		Post-mapping field name.
	 */
	public void addField(String ownerName, String desc, String oldName, String newName) {
		fields.computeIfAbsent(ownerName, n -> new ArrayList<>())
				.add(new FieldMapping(ownerName, oldName, desc, newName));
	}

	/**
	 * @param ownerName
	 * 		Name of class defining the method.
	 * @param desc
	 * 		Descriptor type of the method.
	 * @param oldName
	 * 		Pre-mapping method name.
	 * @param newName
	 * 		Post-mapping method name.
	 */
	public void addMethod(String ownerName, String desc, String oldName, String newName) {
		methods.computeIfAbsent(ownerName, n -> new ArrayList<>())
				.add(new MethodMapping(ownerName, oldName, desc, newName));
	}

	/**
	 * @param ownerName
	 * 		Name of class defining the method.
	 * @param methodName
	 * 		Pre-mapping method name.
	 * @param methodDesc
	 * 		Descriptor type of the method.
	 * @param desc
	 * 		Variable descriptor.
	 * @param oldName
	 * 		Variable old name.
	 * @param index
	 * 		Variable index.
	 * @param newName
	 * 		Post-mapping method name.
	 */
	public void addVariable(String ownerName, String methodName, String methodDesc,
							String desc, String oldName, int index,
							String newName) {
		String key = varKey(ownerName, methodName, methodDesc);
		variables.computeIfAbsent(key, n -> new ArrayList<>())
				.add(new VariableMapping(ownerName, methodName, methodDesc, desc, oldName, index, newName));
	}

	/**
	 * @return Names of classes with mappings.
	 */
	public Set<String> getClassesWithMappings() {
		Set<String> set = new TreeSet<>();
		set.addAll(classes.keySet());
		set.addAll(fields.keySet());
		set.addAll(methods.keySet());
		return set;
	}

	/**
	 * @return Class mappings.
	 */
	public Map<String, ClassMapping> getClasses() {
		return classes;
	}

	/**
	 * @return Field mappings by owner type.
	 */
	public Map<String, List<FieldMapping>> getFields() {
		return fields;
	}

	/**
	 * @return Method mappings by owner type.
	 */
	public Map<String, List<MethodMapping>> getMethods() {
		return methods;
	}

	/**
	 * @return Variable mappings by declaring method type.
	 */
	public Map<String, List<VariableMapping>> getVariables() {
		return variables;
	}

	/**
	 * @param name
	 * 		Pre-mapping name.
	 *
	 * @return Mapping instance of class. May be {@code null}.
	 */
	public ClassMapping getClassMapping(String name) {
		return classes.get(name);
	}

	/**
	 * @param name
	 * 		Declaring class name.
	 *
	 * @return List of field mapping instances.
	 */
	public List<FieldMapping> getClassFieldMappings(String name) {
		return fields.getOrDefault(name, Collections.emptyList());
	}

	/**
	 * @param name
	 * 		Declaring class name.
	 *
	 * @return List of method mapping instances.
	 */
	public List<MethodMapping> getClassMethodMappings(String name) {
		return methods.getOrDefault(name, Collections.emptyList());
	}

	/**
	 * @param ownerName
	 * 		Declaring class name.
	 * @param methodName
	 * 		Declaring method name.
	 * @param methodDesc
	 * 		Declaring method descriptor.
	 *
	 * @return List of field mapping instances.
	 */
	public List<VariableMapping> getMethodVariableMappings(String ownerName, String methodName, String methodDesc) {
		return variables.getOrDefault(varKey(ownerName, methodName, methodDesc), Collections.emptyList());
	}

	@Override
	public String getMappedClassName(String internalName) {
		ClassMapping mapping = classes.get(internalName);
		if (mapping == null)
			return null;
		return mapping.getNewName();
	}

	@Override
	public String getMappedFieldName(String ownerName, String fieldName, String fieldDesc) {
		List<FieldMapping> fieldInClass = getClassFieldMappings(ownerName);
		for (FieldMapping field : fieldInClass)
			if (Objects.equals(fieldDesc, field.getDesc()) && field.getOldName().equals(fieldName))
				return field.getNewName();
		return null;
	}

	@Override
	public String getMappedMethodName(String ownerName, String methodName, String methodDesc) {
		List<MethodMapping> methodsInClass = getClassMethodMappings(ownerName);
		for (MethodMapping method : methodsInClass)
			if (methodDesc.equals(method.getDesc()) && method.getOldName().equals(methodName))
				return method.getNewName();
		return null;
	}

	@Override
	public String getMappedVariableName(String className, String methodName, String methodDesc,
										String name, String desc, int index) {
		List<VariableMapping> variablesInMethod = getMethodVariableMappings(className, methodName, methodDesc);
		for (VariableMapping variable : variablesInMethod) {
			if (equalsOrNull(desc, variable.getDesc()) && equalsOrNull(name, variable.getOldName())
					&& indexEqualsOrOOB(index, variable.getIndex())) {
				return variable.getNewName();
			}
		}
		return null;
	}

	@Override
	public String implementationName() {
		return "INTERMEDIATE";
	}

	@Override
	public void parse(String mappingsText) {
		// no-op
	}

	@Override
	public boolean supportsExportText() {
		return false;
	}

	@Override
	public boolean supportsExportIntermediate() {
		return true;
	}

	@Override
	public String exportText() {
		return null;
	}

	@Override
	public IntermediateMappings exportIntermediate() {
		return this;
	}

	@Override
	public void importIntermediate(IntermediateMappings mappings) {
		// This is never used for intermediates, so we don't need to implement it.
		// If somebody wants to, feel free to paste from MappingsAdapter.
	}

	private static String varKey(String ownerName, String methodName, String methodDesc) {
		return String.format("%s\t%s\t%s", ownerName, methodName, methodDesc);
	}

	private static boolean indexEqualsOrOOB(int a, int b) {
		return a < 0 || b < 0 || a == b;
	}

	private static boolean equalsOrNull(String a, String b) {
		return a == null || b == null || a.equals(b);
	}
}
