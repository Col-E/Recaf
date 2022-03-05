package me.coley.recaf.scripting.impl;

import me.coley.recaf.search.NumberMatchMode;
import me.coley.recaf.search.Search;
import me.coley.recaf.search.TextMatchMode;
import me.coley.recaf.search.result.Result;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

import java.util.List;

/**
 * Utility functions for making searches.
 *
 * @author Wolfie / win32kbase
 */
public class SearchAPI {
	/**
	 * @param resource
	 * 		Resource to search in.
	 * @param owner
	 * 		Name of class. Optional.
	 * @param name
	 * 		Name of field or method. Optional.
	 * @param desc
	 * 		Descriptor of field or method. Optional.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findDeclaration(Resource resource, String owner, String name, String desc, TextMatchMode mode) {
		Search search = new Search();
		search.declaration(owner, name, desc, mode);
		return search.run(resource);
	}

	/**
	 * @param owner
	 * 		Name of class. Optional.
	 * @param name
	 * 		Name of field or method. Optional.
	 * @param desc
	 * 		Descriptor of field or method. Optional.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findDeclaration(String owner, String name, String desc, TextMatchMode mode) {
		return findDeclaration(WorkspaceAPI.getPrimaryResource(), owner, name, desc, mode);
	}

	/**
	 * @param workspace
	 * 		Workspace to search in.
	 * @param owner
	 * 		Name of class. Optional.
	 * @param name
	 * 		Name of field or method. Optional.
	 * @param desc
	 * 		Descriptor of field or method. Optional.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findDeclaration(Workspace workspace, String owner, String name, String desc, TextMatchMode mode) {
		return findDeclaration(workspace.getResources().getPrimary(), owner, name, desc, mode);
	}

	/**
	 * @param resource
	 * 		Resource to search in.
	 * @param owner
	 * 		Name of class. Optional.
	 * @param name
	 * 		Name of field or method. Optional.
	 * @param desc
	 * 		Descriptor of field or method. Optional.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findReference(Resource resource, String owner, String name, String desc, TextMatchMode mode) {
		Search search = new Search();
		search.reference(owner, name, desc, mode);
		return search.run(resource);
	}

	/**
	 * @param owner
	 * 		Name of class. Optional.
	 * @param name
	 * 		Name of field or method. Optional.
	 * @param desc
	 * 		Descriptor of field or method. Optional.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findReference(String owner, String name, String desc, TextMatchMode mode) {
		return findReference(WorkspaceAPI.getPrimaryResource(), owner, name, desc, mode);
	}

	/**
	 * @param workspace
	 * 		Workspace to search in.
	 * @param owner
	 * 		Name of class. Optional.
	 * @param name
	 * 		Name of field or method. Optional.
	 * @param desc
	 * 		Descriptor of field or method. Optional.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findReference(Workspace workspace, String owner, String name, String desc, TextMatchMode mode) {
		return findReference(workspace.getResources().getPrimary(), owner, name, desc, mode);
	}

	/**
	 * @param resource
	 * 		Resource to search in.
	 * @param query
	 * 		Value to search for.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findText(Resource resource, String query, TextMatchMode mode) {
		Search search = new Search();
		search.text(query, mode);
		return search.run(resource);
	}

	/**
	 * @param query
	 * 		Value to search for.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findText(String query, TextMatchMode mode) {
		return findText(WorkspaceAPI.getPrimaryResource(), query, mode);
	}

	/**
	 * @param workspace
	 * 		Workspace to search in.
	 * @param query
	 * 		Value to search for.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findText(Workspace workspace, String query, TextMatchMode mode) {
		return findText(workspace.getResources().getPrimary(), query, mode);
	}

	/**
	 * @param resource
	 * 		Resource to search in.
	 * @param query
	 * 		Value to search for.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findNumber(Resource resource, int query, NumberMatchMode mode) {
		Search search = new Search();
		search.number(query, mode);
		return search.run(resource);
	}

	/**
	 * @param resource
	 * 		Resource to search in.
	 * @param query
	 * 		Value to search for.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findNumber(Resource resource, long query, NumberMatchMode mode) {
		Search search = new Search();
		search.number(query, mode);
		return search.run(resource);
	}

	/**
	 * @param resource
	 * 		Resource to search in.
	 * @param query
	 * 		Value to search for.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findNumber(Resource resource, double query, NumberMatchMode mode) {
		Search search = new Search();
		search.number(query, mode);
		return search.run(resource);
	}

	/**
	 * @param query
	 * 		Value to search for.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findNumber(int query, NumberMatchMode mode) {
		return findNumber(WorkspaceAPI.getPrimaryResource(), query, mode);
	}

	/**
	 * @param query
	 * 		Value to search for.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findNumber(long query, NumberMatchMode mode) {
		return findNumber(WorkspaceAPI.getPrimaryResource(), query, mode);
	}

	/**
	 * @param query
	 * 		Value to search for.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findNumber(double query, NumberMatchMode mode) {
		return findNumber(WorkspaceAPI.getPrimaryResource(), query, mode);
	}

	/**
	 * @param workspace
	 * 		Workspace to search in.
	 * @param query
	 * 		Value to search for.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findNumber(Workspace workspace, int query, NumberMatchMode mode) {
		return findNumber(workspace.getResources().getPrimary(), query, mode);
	}

	/**
	 * @param workspace
	 * 		Workspace to search in.
	 * @param query
	 * 		Value to search for.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findNumber(Workspace workspace, long query, NumberMatchMode mode) {
		return findNumber(workspace.getResources().getPrimary(), query, mode);
	}

	/**
	 * @param workspace
	 * 		Workspace to search in.
	 * @param query
	 * 		Value to search for.
	 * @param mode
	 * 		Search mode.
	 *
	 * @return List of search results.
	 */
	public static List<Result> findNumber(Workspace workspace, double query, NumberMatchMode mode) {
		return findNumber(workspace.getResources().getPrimary(), query, mode);
	}
}
