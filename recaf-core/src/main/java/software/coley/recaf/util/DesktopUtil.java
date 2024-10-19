package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import software.coley.recaf.analytics.SystemInformation;

import java.awt.Dimension;
import java.awt.Toolkit;
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
		final Runtime rt = Runtime.getRuntime();
		switch (PlatformType.get()) {
			case MAC -> rt.exec(new String[]{"open", uri.toString()});
			case WINDOWS -> rt.exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", uri.toString()});
			case LINUX -> {
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
			default -> throw new IllegalStateException("Unsupported OS: " + SystemInformation.OS_NAME);
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
