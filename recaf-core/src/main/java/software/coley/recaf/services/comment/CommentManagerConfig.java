package software.coley.recaf.services.comment;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;
import software.coley.recaf.services.decompile.Decompiler;

/**
 * Config for {@link CommentManager}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class CommentManagerConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableBoolean enableCommentDisplay = new ObservableBoolean(true);
	private final ObservableInteger wordWrappingLimit = new ObservableInteger(100);

	@Inject
	public CommentManagerConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, CommentManager.SERVICE_ID + CONFIG_SUFFIX);
		// Add values
		addValue(new BasicConfigValue<>("enable-display", boolean.class, enableCommentDisplay));
		addValue(new BasicConfigValue<>("word-wrapping-limit", int.class, wordWrappingLimit));
	}

	/**
	 * @return {@code true} when comments should be enabled in {@link Decompiler} output.
	 */
	@Nonnull
	public ObservableBoolean getEnableCommentDisplay() {
		return enableCommentDisplay;
	}

	/**
	 * @return Number of characters to allow before line wrapping a comment.
	 */
	@Nonnull
	public ObservableInteger getWordWrappingLimit() {
		return wordWrappingLimit;
	}
}
