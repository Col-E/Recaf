package software.coley.recaf.services.text;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.util.EscapeUtil;
import software.coley.recaf.util.StringUtil;

/**
 * Config for text formatting.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TextFormatConfig extends BasicConfigContainer {
	public static final String ID = "text-format";
	private final ObservableBoolean escape = new ObservableBoolean(true);
	private final ObservableBoolean shorten = new ObservableBoolean(true);
	private final ObservableInteger maxLength = new ObservableInteger(120);

	@Inject
	public TextFormatConfig() {
		super(ConfigGroups.SERVICE_UI, ID + CONFIG_SUFFIX);
		// Add values
		addValue(new BasicConfigValue<>("escape", boolean.class, escape));
		addValue(new BasicConfigValue<>("shorten", boolean.class, shorten));
		addValue(new BasicConfigValue<>("max-length", int.class, maxLength));
	}

	/**
	 * @return {@code true} to escape text with {@link #filter(String)}.
	 */
	@Nonnull
	public ObservableBoolean getDoEscape() {
		return escape;
	}

	/**
	 * @return {@code true} to shorten path text with {@link #filter(String)}.
	 */
	@Nonnull
	public ObservableBoolean getDoShortenPaths() {
		return shorten;
	}

	/**
	 * @return {@code true} to limit the length of text with {@link #filter(String)}.
	 */
	@Nonnull
	public ObservableInteger getMaxLength() {
		return maxLength;
	}

	/**
	 * @param string
	 * 		Some text to filter.
	 *
	 * @return Filtered text based on current config.
	 */
	public String filter(@Nullable String string) {
		return filter(string, true, true, true);
	}

	/**
	 * @param string
	 * 		Some text to filter.
	 * @param shortenPath
	 * 		Apply path shortening filtering.
	 * @param escape
	 * 		Apply escaping.
	 * @param maxLength
	 * 		Apply max length cap.
	 *
	 * @return Filtered text based on current config.
	 */
	public String filter(@Nullable String string, boolean shortenPath, boolean escape, boolean maxLength) {
		if (string == null) return null;
		if (shortenPath) string = filterShorten(string);
		if (escape) string = filterEscape(string);
		if (maxLength) string = filterMaxLength(string);
		return string;
	}

	/**
	 * @param string
	 * 		Some text to filter.
	 *
	 * @return Filtered text based on current config.
	 */
	public String filterShorten(@Nullable String string) {
		if (string != null && shorten.getValue())
			return StringUtil.shortenPath(string);
		return string;
	}

	/**
	 * @param string
	 * 		Some text to filter.
	 *
	 * @return Filtered text based on current config.
	 */
	public String filterEscape(@Nullable String string) {
		if (string != null && escape.getValue())
			return EscapeUtil.escapeAll(string);
		return string;
	}

	/**
	 * @param string
	 * 		Some text to filter.
	 *
	 * @return Filtered text based on current config.
	 */
	public String filterMaxLength(@Nullable String string) {
		if (string != null && maxLength.getValue() != null) {
			int maxLengthPrim = maxLength.getValue();
			if (string.length() > maxLengthPrim)
				string = string.substring(0, maxLengthPrim) + "…";
		}
		return string;
	}
}
