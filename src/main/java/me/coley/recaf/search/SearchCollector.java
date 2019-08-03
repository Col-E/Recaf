package me.coley.recaf.search;

import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Search result collector.
 *
 * <br><br>
 *
 * <b>Search API TODO List:</b>
 * <ul>
 * <li>Member references in more obscure cases</li>
 * <li>Method inheritance (child of given)</li>
 * <li>Instruction text match (depends on text disassembler being finished)</li>
 * <li>Smart optimization, skip certain visitor-api calls if we know our queries won't need to
 * look there</li>
 * </ul>
 *
 * @author Matt
 */
public class SearchCollector {
	public static final int ACC_NOT_FOUND = -1;
	private final Map<Query, List<SearchResult>> results = new LinkedHashMap<>();
	private final Workspace workspace;
	private final Collection<Query> queries;

	/**
	 * Constructs a class search visitor.
	 *
	 * @param workspace
	 * 		Workspace to pull additional references from.
	 * @param queries
	 * 		Queries to check for collecting results.
	 */
	public SearchCollector(Workspace workspace, Collection<Query> queries) {
		this.workspace = workspace;
		this.queries = queries;
	}

	/**
	 * @return Map of queries to their results.
	 */
	public Map<Query, List<SearchResult>> getResultsMap() {
		return results;
	}

	/**
	 * @return Flattened list of the {@link #getResultsMap() result map}.
	 */
	public List<SearchResult> getAllResults() {
		if (results.isEmpty())
			return Collections.emptyList();
		return results.values().stream().reduce((a, b) -> {
			a.addAll(b);
			return a;
		}).get();
	}

	/**
	 * @param clazz
	 * 		Query class reference.
	 * @param <T>
	 * 		Kind of query class.
	 *
	 * @return Stream of queries matching the given class.
	 */
	<T extends Query> Stream<T> queries(Class<T> clazz) {
		return queries.stream()
				.filter(clazz::isInstance)
				.map(clazz::cast);
	}

	/**
	 * Adds all results from the query to the {@link #getResultsMap() results map}.
	 *
	 * @param context
	 * 		Optional context to add to results.
	 * @param quert
	 * 		Query with results to add.
	 */
	void addMatched(Context<?> context, Query quert) {
		List<SearchResult> matched = quert.getMatched();
		if(context != null)
			matched.forEach(res -> res.setContext(context));
		results.computeIfAbsent(quert, p -> new ArrayList<>()).addAll(matched);
		matched.clear();
	}

	// We use suppliers so that we don't have to lookup this information unless
	// we are sure that there is a match and this information is needed.
	// Looking this up in hundreds of cases where we don't need it would just waste time.

	Supplier<Integer> getAccess(String owner, String name, String desc) {
		return () -> acc(owner, name, desc, ACC_NOT_FOUND);
	}

	Supplier<Integer> getAccess(String name, int defaultAcc) {
		return () -> acc(name, defaultAcc);
	}

	private int acc(String name, int defaultAcc) {
		// Descriptor format
		if(name.endsWith(";"))
			throw new IllegalStateException("Must use internal name, not descriptor!");
		// Get access
		if(workspace.hasClass(name))
			return workspace.getClassReader(name).getAccess();
		// Unknown
		return defaultAcc;
	}

	private int acc(String owner, String name, String desc, int defaultAcc) {
		if(workspace.hasClass(owner))
			if(desc.contains("(")) {
				ClassReader reader = workspace.getClassReader(owner);
				MethodNode node = ClassUtil.getMethod(
						reader, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG, name, desc);
				if(node != null)
					return node.access;
				else  {
					// Try and look in parent classes for the method definition
					int ret = acc(reader.getSuperName(), name, desc, defaultAcc);
					if (ret != defaultAcc)
						return ret;
					for(String itf : reader.getInterfaces()) {
						ret = acc(itf, name, desc, defaultAcc);
						if(ret != defaultAcc)
							return ret;
					}
				}
			} else {
				FieldNode node = ClassUtil.getField(
						workspace.getClassReader(owner),ClassReader.SKIP_DEBUG, name, desc);
				if(node != null)
					return node.access;
			}
		return defaultAcc;
	}
}
