package software.coley.recaf.services.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.mapping.format.MappingFileFormat;

/**
 * Outline of intermediate mappings, allowing for clear retrieval regardless of internal storage of mappings.
 * <br>
 * <h2>Relevant noteworthy points</h2>
 * <b>Incomplete mappings</b>: When imported from a {@link MappingFileFormat} not all formats are made equal.
 * Some contain less information than others. See the note in {@link MappingFileFormat} for more information.
 * <br><br>
 * <b>Member references pointing to child sub-types</b>: References to class members can point to child sub-types of
 * the class that defines the member. You may need to check the owner's type hierarchy to see if the field or method
 * is actually defined by a parent class.
 *
 * @author Matt Coley
 */
public interface Mappings {
	/**
	 * Some mapping formats do not include field types since name overloading is illegal at the source level of Java.
	 * It's valid in the bytecode but the mapping omits this info since it isn't necessary information for mapping
	 * that does not support name overloading.
	 * <p/>
	 * This is mostly only relevant for usage of {@link MappingsAdapter} which
	 *
	 * @return {@code true} when field mappings include the type descriptor in their lookup information.
	 */
	default boolean doesSupportFieldTypeDifferentiation() {
		return true;
	}

	/**
	 * Some mapping formats do not include variable types since name overloading is illegal at the source level of Java.
	 * Variable names are not used by the JVM at all so their names can be anything at the bytecode level. So including
	 * the type makes it easier to reverse mappings.
	 *
	 * @return {@code true} when variable mappings include the type descriptor in their lookup information.
	 */
	default boolean doesSupportVariableTypeDifferentiation() {
		return true;
	}

	/**
	 * @param classInfo
	 * 		Class to lookup.
	 *
	 * @return Mapped name of the class, or {@code null} if no mapping exists.
	 */
	@Nullable
	default String getMappedClassName(@Nonnull ClassInfo classInfo) {
		return getMappedClassName(classInfo.getName());
	}

	/**
	 * @param internalName
	 * 		Original class's internal name.
	 *
	 * @return Mapped name of the class, or {@code null} if no mapping exists.
	 */
	@Nullable
	String getMappedClassName(@Nonnull String internalName);

	/**
	 * @param owner
	 * 		Class declaring the field.<br>
	 * 		<b>NOTE</b>: References to class members can point to child sub-types of the class that defines the member.
	 * 		You may need to check the owner's type hierarchy to see if the field is actually defined in a parent class.
	 * @param field
	 * 		Field to lookup.
	 *
	 * @return Mapped name of the field, or {@code null} if no mapping exists.
	 */
	@Nullable
	default String getMappedFieldName(@Nonnull ClassInfo owner, @Nonnull FieldMember field) {
		return getMappedFieldName(owner.getName(), field.getName(), field.getDescriptor());
	}

	/**
	 * @param ownerName
	 * 		Internal name of the class defining the field.<br>
	 * 		<b>NOTE</b>: References to class members can point to child sub-types of the class that defines the member.
	 * 		You may need to check the owner's type hierarchy to see if the field is actually defined in a parent class.
	 * @param fieldName
	 * 		Name of the field.
	 * @param fieldDesc
	 * 		Descriptor of the field.
	 *
	 * @return Mapped name of the field, or {@code null} if no mapping exists.
	 */
	@Nullable
	String getMappedFieldName(@Nonnull String ownerName, @Nonnull String fieldName, @Nonnull String fieldDesc);

	/**
	 * @param owner
	 * 		Class declaring the method.<br>
	 * 		<b>NOTE</b>: References to class members can point to child sub-types of the class that defines the member.
	 * 		You may need to check the owner's type hierarchy to see if the field is actually defined in a parent class.
	 * @param method
	 * 		Method to lookup.
	 *
	 * @return Mapped name of the method, or {@code null} if no mapping exists.
	 */
	@Nullable
	default String getMappedMethodName(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {
		return getMappedMethodName(owner.getName(), method.getName(), method.getDescriptor());
	}

	/**
	 * @param ownerName
	 * 		Internal name of the class defining the method.<br>
	 * 		<b>NOTE</b>: References to class members can point to child sub-types of the class that defines the member.
	 * 		You may need to check the owner's type hierarchy to see if the field is actually defined in a parent class.
	 * @param methodName
	 * 		Name of the method.
	 * @param methodDesc
	 * 		Descriptor of the method.
	 *
	 * @return Mapped name of the method, or {@code null} if no mapping exists.
	 */
	@Nullable
	String getMappedMethodName(@Nonnull String ownerName, @Nonnull String methodName, @Nonnull String methodDesc);

	/**
	 * @param className
	 * 		Internal name of the class defining the method the variable resides in.
	 * @param methodName
	 * 		Name of the method.
	 * @param methodDesc
	 * 		Descriptor of the method.
	 * @param name
	 * 		Name of the variable.
	 * @param desc
	 * 		Descriptor of the variable.
	 * @param index
	 * 		Index of the variable.
	 *
	 * @return Mapped name of the variable, or {@code null} if no mapping exists.
	 */
	@Nullable
	String getMappedVariableName(@Nonnull String className, @Nonnull String methodName, @Nonnull String methodDesc,
								 @Nullable String name, @Nullable String desc, int index);

	/**
	 * Generally this is implemented under the assumption that {@link Mappings} is used to model data explicitly.
	 * For instance, if we have a workspace with a class {@code Person} using this we can see the {@code Person}
	 * in the resulting {@link IntermediateMappings#getClasses()}.
	 * <br>
	 * However, when {@link Mappings} is used to pattern-match and replace <i>(Like replacing a prefix/package
	 * in a class name)</i> then there is no way to model this since we don't know all possible matches beforehand.
	 * In such cases, we should <i>avoid using this method</i>.
	 * But for API consistency an empty {@link IntermediateMappings} should be returned.
	 *
	 * @return Object representation of mappings.
	 */
	@Nonnull
	IntermediateMappings exportIntermediate();
}