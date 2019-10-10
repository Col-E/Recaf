package me.coley.recaf.config;

import me.coley.recaf.util.LangUtil;

/**
 * Display configuration.
 *
 * @author Matt
 */
public class ConfDisplay extends Config {
	/**
	 * UI language.
	 */
	@Conf("display.language")
	public String language = LangUtil.DEFAULT_LANGUAGE;
	/**
	 * Stylesheet group to use.
	 */
	@Conf("display.style")
	public String style = "base";

	ConfDisplay() {
		super("display");
	}

	@Override
	protected void onLoad() {
		LangUtil.load(language);
	}
}
