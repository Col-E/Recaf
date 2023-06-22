package software.coley.recaf.ui;

import atlantafx.base.theme.Theme;
import org.jetbrains.annotations.Nullable;

/**
 * AtlantaFX Recaf theme.
 *
 * @author Matt Coley
 */
public class RecafTheme implements Theme {
	@Override
	public String getName() {
		return "recaf";
	}

	@Override
	public String getUserAgentStylesheet() {
		return "/style/recaf.css";
	}

	@Nullable
	@Override
	public String getUserAgentStylesheetBSS() {
		// Not used
		return null;
	}

	@Override
	public boolean isDarkMode() {
		return true;
	}
}
