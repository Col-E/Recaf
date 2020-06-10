package me.coley.recaf.search;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.stream.Stream;

import static org.objectweb.asm.ClassReader.*;

/*
 * TODO with Search API:
 *  - Method inheritance (child of given)
 *  - Strings in odd places (dynamic instruction arguments)
 *  - Not internal to the API, but supply a way to handle "\\uXXXX" (unicode search)
 *  - Smart optimization, skip certain visitor-api calls if we know our queries won't need to look there
 */
/**
 * Search result collector.
 *
 * @author Matt
 */
public class SearchCollector {
	public static final int ACC_NOT_FOUND = 0;
	private final ListMultimap<Query, SearchResult> results = MultimapBuilder
			.linkedHashKeys(2).arrayListValues().build();
	private final Map<Query, List<SearchResult>> resultMapView = Multimaps.asMap(results);
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
	public ListMultimap<Query, SearchResult> getResultsMap() {
		return results;
	}

	/**
	 * @return Flattened list of the {@link #getResultsMap() result map} containing entries shared
	 * among multiple queries that are similar.
	 * @see SearchResult#isContextSimilar(SearchResult)
	 */
	public List<SearchResult> getOverlappingResults() {
		// Get results of multiple queries that are similar
		return resultMapView.values().stream()
				// Cast the stream to Collection for compatibility with LinkedHashSet
				.map((Function<List<?>, Collection<SearchResult>>) Collection.class::cast)
				.reduce((a, b) -> {
					Set<SearchResult> overlapping = new LinkedHashSet<>(Math.min(a.size(), b.size()));
					for (SearchResult resultA : a) {
						for (SearchResult resultB : b) {
							if (resultA.isContextSimilar(resultB) || (
								resultA.getContext().contains(resultB.getContext()) ||
								resultB.getContext().contains(resultA.getContext()))) {
								overlapping.add(resultA);
								overlapping.add(resultB);
							}
						}
					}
					return overlapping;
				})
				// Cast the Optional to List for compatibility with Collections.emptyList()
				.map((Function<Collection<SearchResult>, List<SearchResult>>) ArrayList::new)
				.orElseGet(Collections::emptyList);
	}

	/**
	 * @return Flattened list of the {@link #getResultsMap() result map}.
	 */
	public List<SearchResult> getAllResults() {
		return new ArrayList<>(results.values());
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
	 * @param query
	 * 		Query with results to add.
	 */
	void addMatched(Context<?> context, Query query) {
		List<SearchResult> matched = query.getMatched();
		if(context == null)
			throw new IllegalStateException("Must have context");
		matched.forEach(res -> res.setContext(context));
		results.putAll(query, matched);
		matched.clear();
	}

	// We use suppliers so that we don't have to lookup this information unless
	// we are sure that there is a match and this information is needed.
	// Looking this up in hundreds of cases where we don't need it would just waste time.

	IntSupplier getAccess(String owner, String name, String desc) {
		return () -> acc(owner, name, desc, ACC_NOT_FOUND);
	}

	IntSupplier getAccess(String name, int defaultAcc) {
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
						reader, SKIP_CODE | SKIP_DEBUG, name, desc);
				if(node != null)
					return node.access;
				// Try and look in parent classes for the method definition
				int ret = acc(reader.getSuperName(), name, desc, defaultAcc);
				if(ret != defaultAcc)
					return ret;
				for(String itf : reader.getInterfaces()) {
					ret = acc(itf, name, desc, defaultAcc);
					if(ret != defaultAcc)
						return ret;
				}
			} else {
				FieldNode node = ClassUtil.getField(
						workspace.getClassReader(owner), SKIP_CODE | SKIP_DEBUG, name, desc);
				if(node != null)
					return node.access;
			}
		return defaultAcc;
	}
}
