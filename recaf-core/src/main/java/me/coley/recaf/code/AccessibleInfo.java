package me.coley.recaf.code;

/**
 * Information for an accessible type (class, field, method, etc)
 *
 * @author Matt Coley
 */
public interface AccessibleInfo {
	/**
	 * @return Type/member access flags.
	 */
	int getAccess();
}
