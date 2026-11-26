package software.coley.recaf.ui.pane.editing.text;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;

/**
 * Config for common text editor capabilities.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TextConfig extends BasicConfigContainer {
	private final ObservableBoolean highlightWord = new ObservableBoolean(true);
	private final ObservableBoolean trackBrackets = new ObservableBoolean(true);

	@Inject
	public TextConfig() {
		super(ConfigGroups.SERVICE_UI, "text" + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("highlight-word", boolean.class, highlightWord));
		addValue(new BasicConfigValue<>("bracket-tracking", boolean.class, trackBrackets));
	}

	/**
	 * @return {@code true} to track brackets and highlight the matching pair.
	 * {@code false} to disable bracket tracking.
	 */
	public boolean doTrackBrackets() {
		return trackBrackets.getValue();
	}

	/**
	 * @return {@code true} to highlight the word under the cursor.
	 * {@code false} to disable word highlighting.
	 */
	public boolean doHighlightWord() {
		return highlightWord.getValue();
	}


}
