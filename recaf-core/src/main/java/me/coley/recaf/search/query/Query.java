package me.coley.recaf.search.query;

import me.coley.recaf.workspace.resource.Resource;

/**
 * Outline of base query behavior. Allows {@link #createVisitor(Resource, QueryVisitor) generation of class visitors}
 * that can scan class bytecode for matches against the query's implementation details.
 *
 * @author Matt Coley
 */
public interface Query {
	/**
	 * @param resource
	 * 		The resource containing the class being visited.
	 * @param delegate
	 * 		An optional parent query visitor, may be {@code null}. Allows for chaining multiple queries.
	 *
	 * @return A class visitor dedicated to matching against the current query implementation.
	 */
	QueryVisitor createVisitor(Resource resource, QueryVisitor delegate);
}
