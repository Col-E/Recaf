package me.coley.recaf.mapping.gen;

import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;

/**
 * Base name generation outline.
 *
 * @author Matt Coley
 */
public interface NameGenerator {
	/**
	 * @param info
	 * 		Class to rename.
	 *
	 * @return New class name.
	 */
	String mapClass(CommonClassInfo info);

	/**
	 * @param owner
	 * 		Class the field is defined in.
	 * @param info
	 * 		Field to rename.
	 *
	 * @return New field name.
	 */
	String mapField(CommonClassInfo owner, FieldInfo info);

	/**
	 * @param owner
	 * 		Class the method is defined in.
	 * @param info
	 * 		Method to rename.
	 *
	 * @return New method name.
	 */
	String mapMethod(CommonClassInfo owner, MethodInfo info);
}
