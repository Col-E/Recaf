package me.coley.recaf.config;

import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.ui.controls.view.FileViewport;
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
	public String style = "default";
	/**
	 * Display mode for classes.
	 */
	@Conf("display.classmode")
	public ClassViewport.ClassMode classEditorMode = ClassViewport.ClassMode.DECOMPILE;
	/**
	 * Display mode for files.
	 */
	@Conf("display.filemode")
	public FileViewport.FileMode fileEditorMode = FileViewport.FileMode.AUTO;

	ConfDisplay() {
		super("display");
	}

	@Override
	protected void onLoad() {
		LangUtil.load(language);
	}
}
