package me.coley.recaf.mapping;

import me.coley.recaf.mapping.format.IntermediateMappings;

/**
 * Outline of all mapping implementations, allowing for clear retrieval regardless of internal storage of mappings.
 * <br>
 * <h2>Relevant noteworthy points</h2>
 * <b>Incomplete mappings</b>: Not all mapping formats are complete in their representation. Some may omit the
 * descriptor of fields <i>(Because at the source level, overloaded names are illegal within the same class)</i>.
 * So while the methods defined here will always be provided all of this information, each implementation may have to
 * do more than a flat one-to-one lookup in these cases.
 * <br><br>
 * <b>Member references pointing to child sub-types</b>: References to class members can point to child sub-types of
 * the class that defines the member. You may need to check the owner's type hierarchy to see if the field or method
 * is actually defined by a parent class.
 * <br><br>
 * <b>Implementations do not need to be complete to partially work</b>: Some mapping formats do not support renaming
 * for variable names in methods. This is fine, because any method in this interface can be implemented as a no-op by
 * returning {@code null}.
 *
 * @author Matt Coley
 */
public interface Mappings {
	/**
	 * @param internalName
	 * 		Original class's internal name.
	 *
	 * @return Mapped name of the class, or {@code null} if no mapping exists.
	 */
	String getMappedClassName(String internalName);

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
	String getMappedFieldName(String ownerName, String fieldName, String fieldDesc);

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
	String getMappedMethodName(String ownerName, String methodName, String methodDesc);

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
	String getMappedVariableName(String className, String methodName, String methodDesc,
								 String name, String desc, int index);

	/**
	 * @return Name of the mapping format implementation.
	 */
	String implementationName();

	/**
	 * @param mappingsText
	 * 		Text of the mappings to parse.
	 */
	void parse(String mappingsText);

	/**
	 * @return {@code true} when exporting the current mappings to text is supported.
	 *
	 * @see #exportText()
	 */
	default boolean supportsExportText() {
		return false;
	}

	/**
	 * @return {@code true} when exporting the current mappings to intermediate is supported.
	 *
	 * @see #exportIntermediate()
	 */
	default boolean supportsExportIntermediate() {
		return false;
	}

	/**
	 * @return Exported mapping text.
	 */
	default String exportText() {
		return null;
	}

	/**
	 * @return Object representation of mappings.
	 *
	 * @see #importIntermediate(IntermediateMappings)
	 */
	IntermediateMappings exportIntermediate();

	/**
	 * @param mappings
	 * 		Object representation of mappings.
	 *
	 * @see #exportIntermediate()
	 */
	void importIntermediate(IntermediateMappings mappings);
}
