package me.coley.recaf.search;

import me.coley.recaf.workspace.Workspace;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Query to find classes inheriting a given class.
 *
 * @author Matt
 */
public class ClassInheritanceQuery extends Query {
	private final Set<String> descendants;

	/**
	 * Constructs an inheritance query.
	 *
	 * @param workspace
	 * 		Workspace to generate hierarchy from.
	 * @param name
	 * 		Class name
	 */
	public ClassInheritanceQuery(Workspace workspace, String name) {
		super(QueryType.CLASS_INHERITANCE, null);
		this.descendants = workspace.getHierarchyGraph().getAllDescendants(name)
				.collect(Collectors.toSet());
	}

	/**
	 * Adds a result if the given class is a descendant of the specified class.
	 *
	 * @param access
	 * 		Class modifiers.
	 * @param name
	 * 		Name of class.
	 */
	public void match(int access, String name) {
		if(descendants.contains(name)) {
			getMatched().add(new ClassResult(access, name));
		}
	}
}
