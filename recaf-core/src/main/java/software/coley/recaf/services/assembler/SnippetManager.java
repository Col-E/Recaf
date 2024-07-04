package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.Service;

import java.util.List;
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
	 * @return List of recorded snippets.
	 */
	@Nonnull
	public List<Snippet> getSnippets() {
		return config.getSnippets().values().stream()
				.sorted(Snippet.NAME_COMPARATOR)
				.toList();
	}

	/**
	 * @param name
	 * 		Name of snippet to look up.
	 *
	 * @return Snippet by the given name, if one exists.
	 */
	@Nullable
	public Snippet getByName(@Nonnull String name) {
		return config.getSnippets().get(name);
	}

	/**
	 * Register or update a snippet.
	 *
	 * @param snippet
	 * 		Snippet to register.
	 * 		If an existing snippet with the same {@link Snippet#name()} exists, it will be replaced.
	 */
	public void putSnippet(@Nonnull Snippet snippet) {
		String name = snippet.name();
		Snippet old = config.getSnippets().put(name, snippet);
		if (snippet.equals(old)) return;
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
		removeSnippet(snippet.name());
	}

	/**
	 * Remove a given snippet by name.
	 *
	 * @param name
	 * 		Snippet name / identifier.
	 */
	public void removeSnippet(@Nonnull String name) {
		Snippet removed = config.getSnippets().remove(name);
		if (removed != null)
			Unchecked.checkedForEach(listeners, listener -> listener.onSnippetRemoved(removed),
					(listener, t) -> logger.error("Exception thrown when removing snippet '{}'", name, t));
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addSnippetListener(@Nonnull SnippetListener listener) {
		listeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	public void removeSnippetListener(@Nonnull SnippetListener listener) {
		listeners.remove(listener);
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
