package software.coley.recaf.services.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.services.mapping.data.ClassMapping;
import software.coley.recaf.services.mapping.data.FieldMapping;
import software.coley.recaf.services.mapping.data.MethodMapping;
import software.coley.recaf.services.mapping.data.VariableMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Collection of object representations of mappings.
 * Useful as an intermediate between multiple types of {@link Mappings}.
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
		if (Objects.equals(oldName, newName)) return; // Skip identity mappings
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
		if (Objects.equals(oldName, newName)) return; // Skip identity mappings
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
		if (Objects.equals(oldName, newName)) return; // Skip identity mappings
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
		if (Objects.equals(oldName, newName)) return; // Skip identity mappings
		String key = varKey(ownerName, methodName, methodDesc);
		variables.computeIfAbsent(key, n -> new ArrayList<>())
				.add(new VariableMapping(ownerName, methodName, methodDesc, desc, oldName, index, newName));
	}

	/**
	 * @return Names of classes with mappings.
	 */
	@Nonnull
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
	@Nonnull
	public Map<String, ClassMapping> getClasses() {
		return classes;
	}

	/**
	 * @return Field mappings by owner type.
	 */
	@Nonnull
	public Map<String, List<FieldMapping>> getFields() {
		return fields;
	}

	/**
	 * @return Method mappings by owner type.
	 */
	@Nonnull
	public Map<String, List<MethodMapping>> getMethods() {
		return methods;
	}

	/**
	 * @return Variable mappings by declaring method type.
	 */
	@Nonnull
	public Map<String, List<VariableMapping>> getVariables() {
		return variables;
	}

	/**
	 * @param name
	 * 		Pre-mapping name.
	 *
	 * @return Mapping instance of class. May be {@code null}.
	 */
	@Nullable
	public ClassMapping getClassMapping(String name) {
		return classes.get(name);
	}

	/**
	 * @param name
	 * 		Declaring class name.
	 *
	 * @return List of field mapping instances.
	 */
	@Nonnull
	public List<FieldMapping> getClassFieldMappings(String name) {
		return fields.getOrDefault(name, Collections.emptyList());
	}

	/**
	 * @param name
	 * 		Declaring class name.
	 *
	 * @return List of method mapping instances.
	 */
	@Nonnull
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
	@Nonnull
	public List<VariableMapping> getMethodVariableMappings(String ownerName, String methodName, String methodDesc) {
		return variables.getOrDefault(varKey(ownerName, methodName, methodDesc), Collections.emptyList());
	}

	@Nullable
	@Override
	public String getMappedClassName(@Nonnull String internalName) {
		ClassMapping mapping = getClassMapping(internalName);
		if (mapping == null)
			return null;
		return mapping.getNewName();
	}

	@Nullable
	@Override
	public String getMappedFieldName(@Nonnull String ownerName, @Nonnull String fieldName, @Nonnull String fieldDesc) {
		List<FieldMapping> fieldInClass = getClassFieldMappings(ownerName);
		for (FieldMapping field : fieldInClass)
			// Some mapping formats exclude descriptors (which sucks) so we bypass the desc check if that is the case.
			if ((field.getDesc() == null || Objects.equals(fieldDesc, field.getDesc())) && field.getOldName().equals(fieldName))
				return field.getNewName();
		return null;
	}

	@Nullable
	@Override
	public String getMappedMethodName(@Nonnull String ownerName, @Nonnull String methodName, @Nonnull String methodDesc) {
		List<MethodMapping> methodsInClass = getClassMethodMappings(ownerName);
		for (MethodMapping method : methodsInClass)
			if (methodDesc.equals(method.getDesc()) && method.getOldName().equals(methodName))
				return method.getNewName();
		return null;
	}

	@Nullable
	@Override
	public String getMappedVariableName(@Nonnull String className, @Nonnull String methodName, @Nonnull String methodDesc,
	                                    @Nullable String name, @Nullable String desc, int index) {
		List<VariableMapping> variablesInMethod = getMethodVariableMappings(className, methodName, methodDesc);
		for (VariableMapping variable : variablesInMethod) {
			if (equalsOrNull(desc, variable.getDesc()) && equalsOrNull(name, variable.getOldName())
					&& indexEqualsOrOOB(index, variable.getIndex())) {
				return variable.getNewName();
			}
		}
		return null;
	}

	@Nonnull
	@Override
	public IntermediateMappings exportIntermediate() {
		return this;
	}

	@Nonnull
	protected static String varKey(@Nonnull String ownerName, @Nonnull String methodName, @Nonnull String methodDesc) {
		return ownerName + "\t" + methodName + "\t" + methodDesc;
	}

	private static boolean indexEqualsOrOOB(int a, int b) {
		return a < 0 || b < 0 || a == b;
	}

	private static boolean equalsOrNull(@Nullable String a, @Nullable String b) {
		return a == null || b == null || a.equals(b);
	}
}
