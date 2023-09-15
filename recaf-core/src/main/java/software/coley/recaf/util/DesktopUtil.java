package software.coley.recaf.util;

import jakarta.annotation.Nonnull;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Platform specific desktop interaction utilities.
 *
 * @author xDark
 * @author Matt Coley
 */
public class DesktopUtil {
	private static final Dimension screenSize;

	/**
	 * @return Screen dimensions.
	 */
	@Nonnull
	public static Dimension getScreenSize() {
		return screenSize;
	}

	/**
	 * Attempts to launch a browser to display a {@link URI}.
	 *
	 * @param uri
	 * 		URI to display.
	 *
	 * @throws IOException
	 * 		If the browser is not found, or it fails
	 * 		to be launched.
	 */
	public static void showDocument(@Nonnull URI uri) throws IOException {
		switch (PlatformType.get()) {
			case MAC -> Runtime.getRuntime().exec("open " + uri);
			case WINDOWS -> Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + uri);
			case LINUX -> {
				Runtime rt = Runtime.getRuntime();
				String[] browsers = new String[]{"xdg-open", "google-chrome", "firefox", "opera", "konqueror", "mozilla"};
				for (String browser : browsers) {
					try (InputStream in = rt.exec(new String[]{"which", browser}).getInputStream()) {
						if (in.read() != -1) {
							rt.exec(new String[]{browser, uri.toString()});
							return;
						}
					}
				}
				throw new IOException("No browser found");
			}
			default -> throw new IllegalStateException("Unsupported OS");
		}
	}

	static {
		try {
			screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		} catch (Exception ex) {
			throw new IllegalStateException("Could not get screen size", ex);
		}
	}
}
