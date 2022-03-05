package me.coley.recaf.assemble.util;

/**
 * Lookup for class inheritance.
 *
 * @author Matt Coley
 */
public interface InheritanceChecker {
	/**
	 * @param class1
	 * 		Some class.
	 * @param class2
	 * 		Another class.
	 *
	 * @return Internal name of common class.
	 */
	String getCommonType(String class1, String class2);
}
