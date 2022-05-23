package me.coley.recaf.ui.plugin;

import dev.xdark.recaf.plugin.PluginManager;

/**
 *  Exception indicating that failure of the download plugin
 *
 * @author xtherk
 */
public class DownloadFailedException extends RuntimeException {
    public DownloadFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
