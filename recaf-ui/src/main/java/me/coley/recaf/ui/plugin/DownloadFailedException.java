package me.coley.recaf.ui.plugin;

public class DownloadFailedException extends RuntimeException {

    public DownloadFailedException() {
        super();
    }

    public DownloadFailedException(String message) {
        super(message);
    }

    public DownloadFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadFailedException(Throwable cause) {
        super(cause);
    }

    protected DownloadFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
