package me.coley.recaf.search;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.search.query.*;
import me.coley.recaf.search.result.Result;
import me.coley.recaf.util.threading.ThreadPoolFactory;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Search API as a builder pattern. Abstracts away usage of {@link QueryVisitor}.
 *
 * @author Matt Coley
 */
public class Search {
	private static final Logger logger = Logging.get(Search.class);
	private final List<Query> queries = new ArrayList<>();

	/**
	 * Add a text search query.
	 *
	 * @param query
	 * 		The text, or pattern depending on the matching mode, to look for.
	 * @param mode
	 * 		The matching strategy of the query against discovered text.
	 *
	 * @return Search builder.
	 */
	public Search text(String query, TextMatchMode mode) {
		queries.add(new TextQuery(query, mode));
		return this;
	}

	/**
	 * Add a numeric search query.
	 *
	 * @param query
	 * 		The numeric value to look for.
	 * @param mode
	 * 		The matching strategy of the query against discovered values.
	 *
	 * @return Search builder.
	 */
	public Search number(Number query, NumberMatchMode mode) {
		queries.add(new NumberQuery(query, mode));
		return this;
	}

	/**
	 * Add a reference search query.
	 *
	 * @param owner
	 * 		The class defining the referenced member.
	 * @param name
	 * 		The name of the referenced member.
	 * @param desc
	 * 		The type descriptor of the referenced member.
	 * @param mode
	 * 		The matching strategy of the query against the reference type texts.
	 *
	 * @return Search builder.
	 */
	public Search reference(String owner, String name, String desc, TextMatchMode mode) {
		queries.add(new ReferenceQuery(owner, name, desc, mode));
		return this;
	}

	/**
	 * Add a declaration search query.
	 *
	 * @param owner
	 * 		The class defining the declared member.
	 * @param name
	 * 		The name of the declared member.
	 * @param desc
	 * 		The type descriptor of the declared member.
	 * @param mode
	 * 		The matching strategy of the query against the declared type texts.
	 *
	 * @return Search builder.
	 */
	public Search declaration(String owner, String name, String desc, TextMatchMode mode) {
		queries.add(new DeclarationQuery(owner, name, desc, mode));
		return this;
	}

	/**
	 * @param resource
	 * 		Resource to search in.
	 *
	 * @return A visitor that will collect search results in visited classes.
	 */
	public QueryVisitor createQueryVisitor(Resource resource) {
		QueryVisitor visitor = null;
		for (Query query : queries)
			visitor = query.createVisitor(resource, visitor);
		return visitor;
	}

	/**
	 * Scan all classes in the given resource and collect the results into a list.
	 *
	 * @param resource
	 * 		Resource to search in.
	 *
	 * @return Results from search.
	 */
	public List<Result> run(Resource resource) {
		// Create the visitor
		QueryVisitor visitor = createQueryVisitor(resource);
		// Do nothing if no queries are provided
		if (visitor == null)
			return Collections.emptyList();
		// Visit all classes and files in the resource and consolidate results
		for (ClassInfo classInfo : resource.getClasses())
			classInfo.getClassReader().accept(visitor, ClassReader.SKIP_FRAMES);
		for (FileInfo fileInfo : resource.getFiles())
			visitor.visitFile(fileInfo);
		// Wrap results in tree-set to sort, then list for index access
		return new ArrayList<>(new TreeSet<>(visitor.getAllResults()));
	}

	/**
	 * Scan all classes in the given resource and collect the results into a list.
	 * Parallelized version of {@link #run(Resource)}.
	 *
	 * @param resource
	 * 		Resource to search in.
	 *
	 * @return Results from search.
	 */
	public List<Result> runParallel(Resource resource) {
		ExecutorService service = ThreadPoolFactory.newCachedThreadPool("Recaf search");
		// Visit all classes in the resource and consolidate results
		Set<Result> results = Collections.synchronizedSet(new TreeSet<>());
		for (ClassInfo classInfo : resource.getClasses().values()) {
			service.execute(() -> {
				QueryVisitor visitor = createQueryVisitor(resource);
				if (visitor != null) {
					classInfo.getClassReader().accept(visitor, ClassReader.SKIP_FRAMES);
					results.addAll(visitor.getAllResults());
				}
			});
		}
		for (FileInfo fileInfo : resource.getFiles().values()) {
			service.execute(() -> {
				QueryVisitor visitor = createQueryVisitor(resource);
				if (visitor != null) {
					visitor.visitFile(fileInfo);
					results.addAll(visitor.getAllResults());
				}
			});
		}
		try {
			service.shutdown();
			service.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException ex) {
			logger.error("Interrupted parallel search!", ex);
		}
		return new ArrayList<>(results);
	}

	@Override
	public String toString() {
		if (queries.isEmpty()) {
			return "[]";
		}
		return queries.stream()
				.map(Object::toString)
				.collect(Collectors.joining(", "));
	}
}
