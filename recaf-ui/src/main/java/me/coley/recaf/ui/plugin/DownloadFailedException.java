package me.coley.recaf.ui.plugin;

/**
 * Exception indicating that failure of the download plugin
 *
 * @author xtherk
 */
public class DownloadFailedException extends RuntimeException {
	public DownloadFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
