package me.coley.recaf.mapping.gen;

import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;

/**
 * Base filter outline. An implementation of a filter would limit the scope of the generated mappings.
 *
 * @author Matt Coley
 */
public abstract class NameGeneratorFilter {
	private final NameGeneratorFilter next;

	/**
	 * @param next
	 * 		Next filter to link. Chaining filters allows for {@code thisFilter && nextFilter}.
	 */
	protected NameGeneratorFilter(NameGeneratorFilter next) {
		this.next = next;
	}

	/**
	 * @param info
	 * 		Class to check.
	 *
	 * @return {@code true} if the generator should create a new name for the class.
	 */
	public boolean shouldMapClass(CommonClassInfo info) {
		return next == null || next.shouldMapClass(info);
	}

	/**
	 * @param owner
	 * 		Class the field is defined in.
	 * @param info
	 * 		Field to check.
	 *
	 * @return {@code true} if the generator should create a new name for the field.
	 */
	public boolean shouldMapField(CommonClassInfo owner, FieldInfo info) {
		return next == null || next.shouldMapField(owner, info);
	}

	/**
	 * @param owner
	 * 		Class the method is defined in.
	 * @param info
	 * 		Method to check.
	 *
	 * @return {@code true} if the generator should create a new name for the method.
	 */
	public boolean shouldMapMethod(CommonClassInfo owner, MethodInfo info) {
		return next == null || next.shouldMapMethod(owner, info);
	}
}
