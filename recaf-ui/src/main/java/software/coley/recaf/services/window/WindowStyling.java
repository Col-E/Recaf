package software.coley.recaf.services.window;

import com.google.common.hash.Hashing;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.instrument.util.Streams;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.file.RecafDirectoriesConfig;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Facilitates adding custom stylesheets to newly made windows.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class WindowStyling implements Service {
	public static final String SERVICE_ID = "window-styling";
	private static final Logger logger = Logging.get(WindowStyling.class);
	private final List<String> uris = new ArrayList<>();
	private final RecafDirectoriesConfig directoriesConfig;
	private final WindowStylingConfig config;

	@Inject
	public WindowStyling(@Nonnull RecafDirectoriesConfig directoriesConfig, @Nonnull WindowStylingConfig config) {
		this.directoriesConfig = directoriesConfig;
		this.config = config;
	}

	/**
	 * Adds a stylesheet path.
	 *
	 * @param stylesheetPath
	 * 		Path to a stylesheet file.
	 */
	public void addStylesheet(@Nonnull Path stylesheetPath) {
		uris.add(stylesheetPath.toUri().toString());
	}

	/**
	 * Adds a stylesheet from the given url.
	 *
	 * @param stylesheetUrl
	 * 		URL to the stylesheet.
	 *
	 * @return {@code false} when the content from the URL could not be loaded.
	 * {@code true} the stylesheet was added successfully.
	 */
	public boolean addStylesheet(@Nonnull URL stylesheetUrl) {
		// TODO: For 'file:/...' just add that url directly by mapping the file to a 'Path'
		//   String protocol = stylesheetUrl.getProtocol();

		// Extract content from URL
		byte[] stylesheetContents;
		String tempStylesheetName;
		try {
			stylesheetContents = Streams.readStream(stylesheetUrl.openStream());
			tempStylesheetName = Hashing.sha256().hashBytes(stylesheetContents) + ".css";
		} catch (IOException ex) {
			logger.error("Failed reading stylesheet from URL: {}", stylesheetUrl, ex);
			return false;
		}

		// Write to temp directory
		Path destinationPath;
		try {
			destinationPath = directoriesConfig.getTempDirectory().resolve(tempStylesheetName);
			Files.write(destinationPath, stylesheetContents);
		} catch (IOException ex) {
			logger.error("Failed reading stylesheet from URL: {}", stylesheetUrl, ex);
			return false;
		}

		// Add the temp path copy to the stylesheets URI list.
		addStylesheet(destinationPath);
		return true;
	}

	/**
	 * @return Registered stylesheet uri's.
	 */
	@Nonnull
	public List<String> getStylesheetUris() {
		return Collections.unmodifiableList(uris);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public WindowStylingConfig getServiceConfig() {
		return config;
	}
}
