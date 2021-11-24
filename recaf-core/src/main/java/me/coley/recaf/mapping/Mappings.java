package me.coley.recaf.mapping;

import java.util.Map;

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
	 * @return The complete mappings as represented in the ASM format.
	 */
	Map<String, String> toAsmFormattedMappings();

	/**
	 * @return Name of the mapping format implementation.
	 */
	String implementationName();

	/**
	 * @param mappingsText
	 * 		The raw text of the mappings to parse.
	 */
	void parse(String mappingsText);

	/**
	 * @return The exported mapping text.
	 */
	String export();
}
