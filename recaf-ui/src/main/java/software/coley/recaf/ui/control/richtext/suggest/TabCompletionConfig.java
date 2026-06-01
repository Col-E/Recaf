package software.coley.recaf.ui.control.richtext.suggest;

import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.observables.ObservableMap;
import software.coley.observables.ObservableObject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.BasicMapConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.config.ConfigComponentManager;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.pane.editing.assembler.AssemblerPane;
import software.coley.recaf.util.Lang;

import java.util.Map;
import java.util.TreeMap;

/**
 * Config for {@link TabCompleter} use cases.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TabCompletionConfig extends BasicConfigContainer {
	private final ObservableObject<PopupPosition> popupPosition = new ObservableObject<>(PopupPosition.ABOVE_RIGHT);
	private final ObservableBoolean enabledInAssembler = new ObservableBoolean(true);
	private final ObservableBoolean enabledInJavaSource = new ObservableBoolean(true);
	private final ObservableInteger maxCompletionRows = new ObservableInteger(15);
	private final ObservableInteger maxCompletionLength = new ObservableInteger(200);
	private final ObservableBoolean adaptiveRankingEnabled = new ObservableBoolean(true);
	private final ObservableBoolean adaptiveRankingLearningEnabled = new ObservableBoolean(true);
	private final CompletionRankingMap adaptiveRankingUsageCounts = new CompletionRankingMap();

	/**
	 * Constructor for unit test usage.
	 */
	@VisibleForTesting
	public TabCompletionConfig() {
		this(null);
	}

	/**
	 * @param componentManager
	 * 		Component manger to register custom config display components with.
	 */
	@Inject
	public TabCompletionConfig(@Nullable ConfigComponentManager componentManager) {
		super(ConfigGroups.SERVICE_UI, "tab-completion" + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("popup-position", PopupPosition.class, popupPosition));
		addValue(new BasicConfigValue<>("enabled-in-assembler", boolean.class, enabledInAssembler));
		addValue(new BasicConfigValue<>("enabled-in-java-source", boolean.class, enabledInJavaSource));
		addValue(new BasicConfigValue<>("max-completion-rows", int.class, maxCompletionRows));
		addValue(new BasicConfigValue<>("max-completion-length", int.class, maxCompletionLength));
		addValue(new BasicConfigValue<>("adaptive-ranking-enabled", boolean.class, adaptiveRankingEnabled));
		addValue(new BasicConfigValue<>("adaptive-ranking-learning-enabled", boolean.class, adaptiveRankingLearningEnabled));
		addValue(new BasicMapConfigValue<>("adaptive-ranking-usage-counts", CompletionRankingMap.class, String.class, Integer.class, adaptiveRankingUsageCounts));

		// Register component for clearing learned adaptive ranking usage counts.
		if (componentManager != null) {
			componentManager.register(this, "adaptive-ranking-usage-counts", true,
					(container, value) -> new ActionButton(Lang.getBinding("service.ui.tab-completion-config.reset-ranking"), this::clearAdaptiveRankingUsageCounts));
		}
	}

	/**
	 * @return Current popup position.
	 */
	@Nonnull
	public PopupPosition getPopupPosition() {
		return popupPosition.getValue();
	}

	/**
	 * @return {@code true} when tab completion in the {@link AssemblerPane} should be registered.
	 */
	public boolean isEnabledInAssembler() {
		return enabledInAssembler.getValue();
	}

	/**
	 * @return {@code true} when tab completion in Java source editors should be registered.
	 */
	public boolean isEnabledInJavaSource() {
		return enabledInJavaSource.getValue();
	}

	/**
	 * @return Number of completions to visually show in a popup/overlay. Always {@code >= 1}.
	 */
	public int getMaxCompletionRows() {
		return Math.max(1, maxCompletionRows.getValue());
	}

	/**
	 * @return Max length of a completion string to allow.
	 */
	public int getMaxCompletionLength() {
		return maxCompletionLength.getValue();
	}

	/**
	 * @return {@code true} when adaptive ranking should be used for completion ordering.
	 */
	public boolean isAdaptiveRankingEnabled() {
		return adaptiveRankingEnabled.getValue();
	}

	/**
	 * @param enabled
	 *        {@code true} to enable adaptive completion ranking.
	 */
	public void setAdaptiveRankingEnabled(boolean enabled) {
		adaptiveRankingEnabled.setValue(enabled);
	}

	/**
	 * @return {@code true} when accepted completions should be recorded for later ranking boosts.
	 */
	public boolean isAdaptiveRankingLearningEnabled() {
		return adaptiveRankingLearningEnabled.getValue();
	}

	/**
	 * @param enabled
	 *        {@code true} to record accepted Java completions for later ranking boosts.
	 */
	public void setAdaptiveRankingLearningEnabled(boolean enabled) {
		adaptiveRankingLearningEnabled.setValue(enabled);
	}

	/**
	 * @param key
	 * 		Stable completion identity key.
	 *
	 * @return Number of recorded accepts for the given completion.
	 */
	public int getAdaptiveRankingUsageCount(@Nonnull String key) {
		return adaptiveRankingUsageCounts.getOrDefault(key, 0);
	}

	/**
	 * @param key
	 * 		Stable completion identity key.
	 */
	public void recordAdaptiveRankingSelection(@Nonnull String key) {
		adaptiveRankingUsageCounts.merge(key, 1, Integer::sum);
	}

	/**
	 * Clears all learned adaptive ranking usage data.
	 */
	public void clearAdaptiveRankingUsageCounts() {
		adaptiveRankingUsageCounts.clear();
	}

	/**
	 * @return Persistent learned usage counts keyed by completion identity.
	 */
	@Nonnull
	public CompletionRankingMap getAdaptiveRankingUsageCounts() {
		return adaptiveRankingUsageCounts;
	}

	/**
	 * Observable map type for adaptive ranking usage counts.
	 */
	public static class CompletionRankingMap extends ObservableMap<String, Integer, Map<String, Integer>> {
		public CompletionRankingMap() {
			super(TreeMap::new);
		}
	}

	public enum PopupPosition {
		/**
		 * Popup appears above and to right of the cursor
		 */
		ABOVE_RIGHT,
		/**
		 * Popup appears below and to right of the cursor
		 */
		BELOW_RIGHT,
		/**
		 * Popup appears above and to left of the cursor
		 */
		ABOVE_LEFT,
		/**
		 * Popup appears below and to left of the cursor
		 */
		BELOW_LEFT;

		public boolean isAbove() {
			return this == ABOVE_RIGHT || this == ABOVE_LEFT;
		}

		public boolean isRight() {
			return this == ABOVE_RIGHT || this == BELOW_RIGHT;
		}
	}
}
