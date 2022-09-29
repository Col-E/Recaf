package me.coley.recaf.code;

import javax.annotation.Nullable;

/**
 * Info for outer-method attribute.
 *
 * @author Amejonah
 */
public class OuterMethodInfo {
	private final String owner;
	@Nullable
	private final String name;
	@Nullable
	private final String descriptor;

	/**
	 * @param owner
	 * 		Internal name of the enclosing class of the class.
	 * @param name
	 * 		The name of the method that contains the class, or {@code null} if the class is
	 * 		not enclosed in a method of its enclosing class.
	 * @param descriptor
	 * 		The descriptor of the method that contains the class, or {@code null} if
	 * 		the class is not enclosed in a method of its enclosing class.
	 */
	public OuterMethodInfo(String owner, @Nullable String name, @Nullable String descriptor) {
		this.owner = owner;
		this.name = name;
		this.descriptor = descriptor;
	}

	/**
	 * @return Internal name of the enclosing class of the class.
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @return The name of the method that contains the class, or {@code null} if the class is
	 * not enclosed in a method of its enclosing class (ex. (static) init block).
	 */
	@Nullable
	public String getName() {
		return name;
	}

	/**
	 * @return The descriptor of the method that contains the class, or {@code null} if
	 * the class is not enclosed in a method of its enclosing class (ex. (static) init block).
	 */
	@Nullable
	public String getDescriptor() {
		return descriptor;
	}
}
