package me.coley.recaf.search;

import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.*;

import java.util.*;

/**
 * Builder for {@link SearchCollector}.
 *
 * @author Matt
 */
public class SearchBuilder {
	private final Workspace workspace;
	private final List<Query> queries = new ArrayList<>();
	private int readFlags = ClassReader.SKIP_FRAMES;
	private Collection<String> skipped = Collections.emptyList();

	private SearchBuilder(Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * @param workspace
	 * 		The workspace to search in. Only uses the primary resource.
	 *
	 * @return Initial builder.
	 */
	public static SearchBuilder in(Workspace workspace) {
		return new SearchBuilder(workspace);
	}

	/**
	 * @param query
	 * 		Query to add to the search.
	 *
	 * @return Builder with additional query.
	 */
	public SearchBuilder query(Query query) {
		queries.add(query);
		return this;
	}

	/**
	 * @return Builder that skips debug information <i>
	 * ({@link org.objectweb.asm.ClassVisitor#visitSource},
	 * {@link org.objectweb.asm.MethodVisitor#visitLocalVariable}, and
	 * {@link org.objectweb.asm.MethodVisitor#visitLineNumber})</i>
	 */
	public SearchBuilder skipDebug() {
		this.readFlags |= ClassReader.SKIP_DEBUG;
		return this;
	}

	/**
	 * @return Builder that skips parsing method bodies.
	 */
	public SearchBuilder skipCode() {
		this.readFlags |= ClassReader.SKIP_CODE;
		return this;
	}

	/**
	 * @param skipped
	 * 		Package prefixes to skip.
	 *
	 * @return Builder that skips classes matching the given packages/prefixes.
	 */
	public SearchBuilder skipPackages(Collection<String> skipped) {
		this.skipped = skipped;
		return this;
	}

	/**
	 * @return SearchCollector from the builder. The search is started by calling this method.
	 */
	public SearchCollector build() {
		SearchCollector collector = new SearchCollector(workspace, queries);
		SearchClassVisitor sv = new SearchClassVisitor(collector);
		workspace.getPrimaryClassReaders().forEach(cr -> {
			String name = cr.getClassName();
			if (skip(name))
				return;
			cr.accept(sv, readFlags);
		});
		return collector;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return {@code true} if the class should be skipped.
	 */
	private boolean skip(String name) {
		return skipped.stream().anyMatch(name::startsWith);
	}
}
