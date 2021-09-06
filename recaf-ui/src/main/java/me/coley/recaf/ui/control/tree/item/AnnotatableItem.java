package me.coley.recaf.ui.control.tree.item;

/**
 * Item that can have an annotation attached to it.
 *
 * @author Matt Coley
 */
public interface AnnotatableItem {
	/**
	 * @return {@code true} when {@link #getAnnotationType()} is not {@code null}.
	 */
	default boolean isAnnotated() {
		return getAnnotationType() != null;
	}

	/**
	 * @param type
	 * 		Annotation type.
	 */
	void setAnnotationType(String type);

	/**
	 * @return The attached annotation type. May be {@code null}.
	 */
	String getAnnotationType();
}
