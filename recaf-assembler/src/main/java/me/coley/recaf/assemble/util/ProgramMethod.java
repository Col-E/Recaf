package me.coley.recaf.assemble.util;

/**
 * Interface to access method information for {@link me.coley.recaf.assemble.suggestions.Suggestions} logic.
 *
 * @author Nowilltolife
 */
public interface ProgramMethod {
	int getAccess();

	String getName();

	String getDescriptor();
}
