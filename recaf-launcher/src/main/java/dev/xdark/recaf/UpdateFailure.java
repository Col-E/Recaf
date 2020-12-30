package dev.xdark.recaf;

/**
 * Wrapper for update failure messages.
 *
 * @author Matt Coley
 * @author xDark
 */
public enum UpdateFailure {
	NO_RELEASE("missingInfo", "Could not find release"),
	NO_ASSET("missingAsset", "Launcher was unable to detect release asset from GitHub releases.\n" +
			"Please open an issue at: \n" + Launcher.ISSUES_URL),
	NO_WRITE_PERMISSION("notWriteable", "Jar is not writeable\n" +
			"Verify that you don't have any running Recaf instances " +
			"and permissions of your file system");

	private final String flagContent;
	private final String logMessage;

	UpdateFailure(String flagContent, String logMessage) {
		this.flagContent = flagContent;
		this.logMessage = logMessage;
	}

	/**
	 * @return Flag to pass to Recaf, indicating update failure.
	 */
	public String getFlagContent() {
		return flagContent;
	}

	/**
	 * @return Log message to print describing the problem.
	 */
	public String getLogMessage() {
		return logMessage;
	}
}
