package software.coley.recaf.ui.config;

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
 * Config for text display.
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
		addValue(new BasicConfigValue<>("escape", Boolean.class, escape));
		addValue(new BasicConfigValue<>("shorten", Boolean.class, shorten));
		addValue(new BasicConfigValue<>("max-length", Integer.class, maxLength));
	}

	/**
	 * @param string
	 * 		Some text to filter.
	 *
	 * @return Filtered text based on current config.
	 */
	public String filter(@Nullable String string) {
		if (string == null) return null;

		if (shorten.getValue())
			string = StringUtil.shortenPath(string);

		if (escape.getValue())
			string = EscapeUtil.escapeAll(string);

		if (maxLength.getValue() != null) {
			int maxLengthPrim = maxLength.getValue();
			if (string.length() > maxLengthPrim)
				string = string.substring(0, maxLengthPrim) + "…";
		}

		return string;
	}
}
