package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.services.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple code snippet manager to hold common assembler samples for common operations.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class SnippetManager implements Service {
	private static final Logger logger = Logging.get(SnippetManager.class);

	public static final String SERVICE_ID = "snippets";
	private final SnippetManagerConfig config;
	private final List<SnippetListener> listeners = new CopyOnWriteArrayList<>();

	@Inject
	public SnippetManager(@Nonnull SnippetManagerConfig config) {
		this.config = config;
	}

	/**
	 * @param target
	 * 		Target assembler dialect.
	 *
	 * @return List of recorded snippets for the given target.
	 */
	@Nonnull
	public List<Snippet> getSnippets(@Nonnull SnippetTarget target) {
		return config.getSnippets().values().stream()
				.filter(snippet -> snippet.target() == target)
				.sorted(Snippet.NAME_COMPARATOR)
				.toList();
	}

	/**
	 * @param target
	 * 		Target assembler dialect.
	 * @param name
	 * 		Snippet name / title.
	 *
	 * @return Snippet by the given target and name, if one exists.
	 */
	@Nullable
	public Snippet getByName(@Nonnull SnippetTarget target, @Nonnull String name) {
		for (Snippet snippet : config.getSnippets().values())
			if (snippet.target() == target && snippet.name().equals(name))
				return snippet;
		return null;
	}

	/**
	 * Register or update a snippet.
	 *
	 * @param snippet
	 * 		Snippet to register.
	 * 		If an existing snippet with the same {@link Snippet#name()}
	 * 		and {@link Snippet#target()} exists, it will be replaced.
	 */
	public void putSnippet(@Nonnull Snippet snippet) {
		Map<String, Snippet> snippets = config.getSnippets();

		// Get existing matching snippet, if one exists.
		Map.Entry<String, Snippet> existing = findByTargetAndName(snippets, snippet.target(), snippet.name());
		String key = existing == null ? createStorageKey(snippets, snippet) : existing.getKey();
		Snippet old = existing == null ? null : existing.getValue();

		// Only replace if the content is different.
		if (snippet.equals(old)) return;
		snippets.put(key, snippet);

		// Notify listeners of the change.
		String name = snippet.name();
		if (old == null)
			Unchecked.checkedForEach(listeners, listener -> listener.onSnippetAdded(snippet),
					(listener, t) -> logger.error("Exception thrown when registering snippet '{}'", name, t));
		else
			Unchecked.checkedForEach(listeners, listener -> listener.onSnippetModified(old, snippet),
					(listener, t) -> logger.error("Exception thrown when updating snippet '{}'", name, t));
	}

	/**
	 * Remove a given snippet.
	 *
	 * @param snippet
	 * 		Snippet to remove.
	 */
	public void removeSnippet(@Nonnull Snippet snippet) {
		removeSnippet(snippet.target(), snippet.name());
	}

	/**
	 * Remove a given snippet by target and name.
	 *
	 * @param target
	 * 		Target assembler dialect.
	 * @param name
	 * 		Snippet name / title.
	 */
	public void removeSnippet(@Nonnull SnippetTarget target, @Nonnull String name) {
		Map<String, Snippet> snippets = config.getSnippets();
		Map.Entry<String, Snippet> entry = findByTargetAndName(snippets, target, name);
		if (entry == null) return;

		Snippet removed = snippets.remove(entry.getKey());
		if (removed != null)
			Unchecked.checkedForEach(listeners, listener -> listener.onSnippetRemoved(removed),
					(listener, t) -> logger.error("Exception thrown when removing snippet '{}'", name, t));
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addSnippetListener(@Nonnull SnippetListener listener) {
		PrioritySortable.add(listeners, listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	public void removeSnippetListener(@Nonnull SnippetListener listener) {
		listeners.remove(listener);
	}

	@Nullable
	private static Map.Entry<String, Snippet> findByTargetAndName(@Nonnull Map<String, Snippet> snippets,
	                                                              @Nonnull SnippetTarget target,
	                                                              @Nonnull String name) {
		for (Map.Entry<String, Snippet> entry : snippets.entrySet()) {
			Snippet snippet = entry.getValue();
			if (snippet.target() == target && snippet.name().equals(name))
				return entry;
		}
		return null;
	}

	@Nonnull
	private static String createStorageKey(@Nonnull Map<String, Snippet> snippets, @Nonnull Snippet snippet) {
		String base = snippet.target().name() + ":" + snippet.name();
		String key = base;
		for (int suffix = 1; snippets.containsKey(key); suffix++)
			key = base + ":" + suffix;
		return key;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public SnippetManagerConfig getServiceConfig() {
		return config;
	}
}
