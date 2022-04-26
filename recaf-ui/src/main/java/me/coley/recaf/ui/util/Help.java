package me.coley.recaf.ui.util;

import me.coley.recaf.util.DesktopUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.net.URI;

/**
 * Help utilities.
 *
 * @author Matt Coley
 */
public class Help {
	private static final Logger logger = Logging.get(Help.class);

	/**
	 * Gotta pump up that member count, right?
	 */
	public static void openDiscord() {
		browse("https://discord.gg/Bya5HaA");
	}

	/**
	 * Star pls.
	 */
	public static void openGithub() {
		browse("https://github.com/Col-E/Recaf");
	}

	/**
	 * Bugs and features I'll get to <i>"eventually(tm)"</i>
	 */
	public static void openGithubIssues() {
		browse("https://github.com/Col-E/Recaf/issues");
	}

	/**
	 * Lets be honest, nobody reads this... <i>Unless we force them to.</i>
	 */
	public static void openDocumentation() {
		browse("https://www.coley.software/Recaf-documentation/");
	}

	private static void browse(String uri) {
		try {
			DesktopUtil.showDocument(new URI(uri));
		} catch (Exception ex) {
			logger.error("Failed to open link", ex);
		}
	}
}
