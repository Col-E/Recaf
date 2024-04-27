package me.coley.recaf.util.self;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import me.coley.recaf.Recaf;
import me.coley.recaf.control.Controller;
import me.coley.recaf.util.LangUtil;
import me.coley.recaf.util.Log;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Utility for updating Recaf.
 *
 * @author Matt
 */
public class SelfUpdater {
	private static final String API = "https://api.github.com/repos/Col-E/Recaf/releases/latest";
	private static final long UPDATER_START_DELAY_MS = 1000;
	private static final String currentVersion = Recaf.VERSION;
	private static boolean disabled;
	private static String latestVersion;
	private static String latestArtifact;
	private static String latestPatchnotes;
	private static Instant latestVersionDate;
	private static int latestArtifactSize;
	private static Controller controller;

	/**
	 * Fetch latest update information.
	 */
	public static void checkForUpdates() {
		// Check if updates are disabled, or if we're not a jar (developer workspace)
		if (disabled || !isJarContext())
			return;
		// Check if update process should run
		if (shouldSkipUpdate())
			return;
		// Get latest data
		try {
			updateCheckDate();
			fetchLatestInfo();
		} catch(IOException ex) {
			Log.error(ex, "Failed to read from release API");
		}
	}

	/**
	 * Fetch the {@link #latestVersion latest version}
	 * and {@link #latestArtifact latest artifact url}.
	 *
	 * @throws IOException
	 * 		When opening a connection to the API url fails.
	 */
	private static void fetchLatestInfo() throws IOException {
		URL updateURL = new URL(API);
		String content = IOUtils.toString(updateURL.openStream(), StandardCharsets.UTF_8);
		JsonObject updateJson = Json.parse(content).asObject();
		// compare versions
		latestVersion = updateJson.getString("tag_name", "2.0.0");
		latestPatchnotes = updateJson.getString("body", "#Error\nCould not fetch update notes.");
		if (isOutdated()) {
			Log.info(LangUtil.translate("update.outdated"));
			JsonArray assets = updateJson.get("assets").asArray();
			for(JsonValue assetValue : assets.values()) {
				JsonObject assetObj = assetValue.asObject();
				String file = assetObj.getString("name", "invalid");
				// Skip non-jars
				if (!file.endsWith(".jar")) {
					continue;
				}
				// Find the largest jar
				int size = assetObj.getInt("size", 0);
				if (size > latestArtifactSize) {
					latestArtifactSize = size;
					String fileURL = assetObj.getString("browser_download_url", null);
					if (fileURL != null)
						latestArtifact = fileURL;
				}
			}
			try {
				String date = updateJson.getString("published_at", null);
				if (date != null)
					latestVersionDate = Instant.parse(date);
			} catch(DateTimeParseException ex) {
				Log.warn("Failed to parse timestamp for latest release");
			}
			if (latestArtifact == null)
				Log.warn(LangUtil.translate("update.fail.nodownload"));
		}
	}

	/**
	 * @return {@code true} when the current version is behind latest version.
	 */
	private static boolean isOutdated() {
		try {
			String[] arrSelf = currentVersion.split("\\.");
			String[] arrLatest = latestVersion.split("\\.");
			if (arrSelf.length == arrLatest.length) {
				for (int i = 0; i < arrSelf.length; i++) {
					int iSelf = Integer.parseInt(arrSelf[i]);
					int iLatest = Integer.parseInt(arrLatest[i]);
					if (iSelf == iLatest) {
						// do nothing
						continue;
					} else {
						return iSelf < iLatest;
					}
				}
			}
		} catch (Exception ex) {
			Log.error(ex, "Failed parsing versions, current is '{}', latest is '{}'", currentVersion, latestArtifact);
		}
		return false;
	}

	/**
	 * @return {@code true} if the update process should be skipped.
	 */
	private static boolean shouldSkipUpdate()  {
		return controller.config().update().shouldSkip();
	}

	/**
	 * Updates the last check time in the config.
	 */
	private static void updateCheckDate() {
		controller.config().update().lastCheck = System.currentTimeMillis();
	}

	/**
	 * Set the controller to load configuration from.
	 *
	 * @param controller
	 * 		Controller to pull config from.
	 */
	public static void setController(Controller controller) {
		SelfUpdater.controller = controller;
	}

	/**
	 * Disable update checks.
	 */
	public static void disable() {
		disabled = true;
	}

	/**
	 * @return {@code true} when Recaf is run from a jar.
	 */
	private static boolean isJarContext() {
		return SelfReferenceUtil.get().isJar();
	}

	/**
	 * @return {@code true} when an update has been detected.
	 */
	public static boolean hasUpdate() {
		return latestArtifact != null;
	}

	/**
	 * @return Most up to date version of Recaf.
	 */
	public static String getLatestVersion() {
		return latestVersion;
	}

	/**
	 * @return URL string to latest artifact of Recaf.
	 */
	public static String getLatestArtifact() {
		return latestArtifact;
	}

	/**
	 * @return Markdown text of latest Recaf update notes.
	 */
	public static String getLatestPatchnotes() {
		return latestPatchnotes;
	}

	/**
	 * @return Time of most recent Recaf update.
	 */
	public static Instant getLatestVersionDate() {
		return latestVersionDate;
	}

	/**
	 * @return Size in bytes of most recent Recaf update.
	 */
	public static int getLatestArtifactSize() {
		return latestArtifactSize;
	}
}
