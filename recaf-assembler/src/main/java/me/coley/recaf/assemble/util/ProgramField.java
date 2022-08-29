package me.coley.recaf.assemble.util;

/**
 * Interface to access field information for {@link me.coley.recaf.assemble.suggestions.Suggestions} logic.
 *
 * @author Nowilltolife
 */
public interface ProgramField {
	int getAccess();

	String getName();

	String getDescriptor();
}
