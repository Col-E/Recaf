package me.coley.recaf;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import me.coley.recaf.config.impl.ConfUpdate;
import me.coley.recaf.util.FileIO;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.SelfReference;
import me.coley.recaf.util.Threads;
import me.coley.recaf.util.Icons;

/**
 * Ensures execution on the latest version.
 * 
 * @author Matt
 */
@SuppressWarnings("unused")
public class Updater {
	private final static String API_LATEST = "https://api.github.com/repos/Col-E/Recaf/releases/latest";

	public static void run(String[] args) {
		// check if updates should occur
		if (!ConfUpdate.instance().shouldCheck()) {
			return;
		}
		try {
			ConfUpdate.instance().lastCheck = System.currentTimeMillis();
			// get self data
			SelfReference self = SelfReference.get();
			if (!self.isJar()) {
				// If the execuatable context is not a runnable jar, do not
				// attempt to update.
				// Methods to do so exist should this functionality be wanted in
				// the future.
				return;
			}
			String selfVersion = Recaf.VERSION;
			// get latest data
			URL updateURL = new URL(API_LATEST);
			String content = Resources.asCharSource(updateURL, Charsets.UTF_8).read();
			JsonObject updateJson = Json.parse(content).asObject();
			// compare versions
			String latestVersion = updateJson.getString("tag_name", "1.0.0");
			String updateNotes = updateJson.getString("body", "#Error\nCould not fetch update notes.");
			if (isOutdated(selfVersion, latestVersion)) {
				if (!consent(selfVersion, latestVersion, updateNotes)) {
					return;
				}
				Logging.info(Lang.get("update.outdated"));
				JsonArray assets = updateJson.get("assets").asArray();
				for (JsonValue assetValue : assets.values()) {
					JsonObject assetObj = assetValue.asObject();
					String file = assetObj.getString("name", "invalid");
					if (!file.endsWith(".jar")) {
						// this is not the right attachment
						continue;
					}
					String fileURL = assetObj.getString("browser_download_url", null);
					if (fileURL == null) {
						// this shouldn't happen, but just in case.
						continue;
					}
					byte[] updateContents = Resources.asByteSource(new URL(fileURL)).read();
					FileIO.writeFile(file, updateContents);
					Logging.error(Lang.get("update.complete"));
					runUpdated(file, args);
					return;
				}
				Logging.warn(Lang.get("update.fail.nodownload"));
				return;
			}
		} catch (URISyntaxException e) {
			Logging.error(Lang.get("update.fail.resolve"));
		} catch (IOException e) {
			Logging.error(Lang.get("update.fail.jarread"));
		} catch (Exception e) {
			Logging.error(Lang.get("update.fail.unknown") + e.getMessage());
		}
		Logging.fine(Lang.get("update.updated"));
	}

	private static boolean consent(String current, String latest, String markdown) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(Lang.get("update.outdated"));
		alert.setContentText(Lang.get("update.consent"));
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Icons.LOGO);

		Parser parser = Parser.builder().build();
		Node document = parser.parse(markdown);
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		String html = renderer.render(document);
		WebView browser = new WebView();
		WebEngine webEngine = browser.getEngine();
		ScrollPane scrollPane = new ScrollPane(browser);
		String css = "* { font-family: Arial, sans-serif; }";
		html = "<head><style>" + css + "</style></head><h1>" + current + " &#8594; " + latest + "<h1>\n<hr>\n" + html;
		webEngine.loadContent(html, "text/html");
		alert.setGraphic(scrollPane);

		Optional<ButtonType> result = alert.showAndWait();
		return result.get() == ButtonType.OK;
	}

	/**
	 * Rerun recaf with updated version.
	 * 
	 * @param file
	 *            Filename of new version.
	 * @param args
	 *            Command line args to pass.
	 * @throws IOException
	 *             Thrown if the process cannot be started.
	 */
	private static void runUpdated(String file, String[] args) throws IOException {
		String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		File currentJar = new File(file);
		// Build process: java -jar recaf.jar -args
		List<String> procArgs = new ArrayList<>();
		procArgs.add(javaBin);
		procArgs.add("-jar");
		procArgs.add(currentJar.getPath());
		for (String arg : args) {
			procArgs.add(arg);
		}
		// run process and terminate current
		new ProcessBuilder(procArgs).start();
		System.out.println("Rerunning");
		System.exit(0);
	}

	/**
	 * @param self
	 *            Current version.
	 * @param latest
	 *            Latest online version.
	 * @return Is current is behind latest.
	 */
	private static boolean isOutdated(String self, String latest) {
		try {
			String[] arrSelf = self.split("\\.");
			String[] arrLatest = latest.split("\\.");
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
