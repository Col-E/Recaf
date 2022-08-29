package me.coley.recaf.assemble.util;

import java.util.List;

/**
 * Interface to access class information for {@link me.coley.recaf.assemble.suggestions.Suggestions} logic.
 *
 * @author Nowilltolife
 */
public interface ProgramClass {
	String getName();

	String getSuperName();

	<T extends ProgramField> List<T> getFields();

	<T extends ProgramMethod> List<T> getMethods();
}
