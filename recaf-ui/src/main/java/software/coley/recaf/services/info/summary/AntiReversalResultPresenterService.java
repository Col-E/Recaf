package software.coley.recaf.services.info.summary;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for UI presenters of anti-reversal analysis results.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AntiReversalResultPresenterService {
	private final Map<String, AntiReversalResultPresenter> presenters = new ConcurrentHashMap<>();

	@Inject
	public AntiReversalResultPresenterService(@Nonnull Instance<AntiReversalResultPresenter> presenters) {
		for (AntiReversalResultPresenter presenter : presenters)
			registerPresenter(presenter);
	}

	/**
	 * Registers a presenter by its analyzer service ID.
	 *
	 * @param presenter
	 * 		Presenter to register.
	 *
	 * @throws IllegalArgumentException
	 * 		When a presenter for the analyzer ID is already registered.
	 */
	public void registerPresenter(@Nonnull AntiReversalResultPresenter presenter) {
		String analyzerId = presenter.getAnalyzerId();
		if (presenters.putIfAbsent(analyzerId, presenter) != null)
			throw new IllegalArgumentException("Presenter for analyzer ID already registered: " + analyzerId);
	}

	/**
	 * Removes a previously registered presenter.
	 *
	 * @param presenter
	 * 		Presenter to remove.
	 *
	 * @return {@code true} when the supplied presenter was previously registered and removed.
	 */
	public boolean removePresenter(@Nonnull AntiReversalResultPresenter presenter) {
		return presenters.remove(presenter.getAnalyzerId(), presenter);
	}

	/**
	 * @return List of presenters ordered by priority and analyzer ID.
	 */
	@Nonnull
	public List<AntiReversalResultPresenter> getPresenters() {
		return presenters.values().stream()
				.sorted(Comparator.comparingInt(AntiReversalResultPresenter::getPriority)
						.thenComparing(AntiReversalResultPresenter::getAnalyzerId))
				.toList();
	}
}
