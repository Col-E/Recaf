package me.coley.recaf.util;

import com.eclipsesource.json.*;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import me.coley.recaf.Logging;
import me.coley.recaf.Recaf;
import me.coley.recaf.config.impl.ConfUpdate;
import me.coley.recaf.ui.component.UpdatePrompt;

import javax.swing.*;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.*;

/**
 * Ensures execution on the latest version.
 * 
 * @author Matt
 */
@SuppressWarnings("unused")
public class Updater {
	private final static String API_LATEST = "https://api.github.com/repos/Col-E/Recaf/releases/latest";

	/**
	 * Download and execute the latest release of Recaf.
	 *
	 * @param args
	 * 			Launch arguments to pass.
	 */
	public static void updateFromRelease(String[] args) {
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
				if (!UpdatePrompt.consent(selfVersion, latestVersion, updateNotes)) {
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
					executeUpdated(file, args);
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


	/**
	 * Update self with proper dependencies for Java 11 and rerun.
	 * @param args
	 * 			Launch arguments to pass.
	 */
	public static void updateViaJdk11Patch(String[] args) {
		File patched = new File(String.format("recaf-%s-patched.jar", Recaf.VERSION));
		// Generate patched jar with proper dependencies
		try {
			if (!patched.exists()) {
				generatePatched(patched);
			}
		} catch(Exception ex) {
			ex.printStackTrace();
			err("Failed auto-patch", ex.toString());
		}
		// Rerun with dependencies
		try {
			executeUpdated(patched.getAbsolutePath(), args);
		} catch(Exception ex) {
			ex.printStackTrace();
			err("Failed re-run", ex.toString());
		}
	}

	private static void generatePatched(File patched) throws IOException, URISyntaxException {
		SelfReference self = SelfReference.get();
		if(!self.isJar()) {
			err("Failed auto-patch", "Could not update self, as was not running as a jar-file.");
			return;
		}
		String selfURL = self.getFile().toURI().toURL().toString().replace("%", "%%");
		String[] dependencies = new String[] {
				selfURL,
				"http://central.maven.org/maven2/org/controlsfx/controlsfx/11.0.0/controlsfx-11.0.0.jar",
				"http://central.maven.org/maven2/org/openjfx/javafx-web/11.0.2/javafx-web-11.0.2-%s.jar",
				"http://central.maven.org/maven2/org/openjfx/javafx-media/11.0.2/javafx-media-11.0.2-%s.jar",
				"http://central.maven.org/maven2/org/openjfx/javafx-controls/11.0.2/javafx-controls-11.0.2-%s.jar",
				"http://central.maven.org/maven2/org/openjfx/javafx-graphics/11.0.2/javafx-graphics-11.0.2-%s.jar",
				"http://central.maven.org/maven2/org/openjfx/javafx-base/11.0.2/javafx-base-11.0.2-%s.jar"
		};
		// Items to blacklist copying from the self-jar
		Collection<String> blacklist = Arrays.asList("controlsfx", "org/controlsfx", "impl");
		// master local containing all dependencies
		Set<String> depLocalNames = new HashSet<>();
		JarOutputStream jos = new JarOutputStream(new FileOutputStream(patched));
		String os = getOSType();
		for (String dependency : dependencies) {
			URL depURL = new URL(String.format(dependency, os));
			File depTemp = File.createTempFile("recaf","dependency");
			Files.copy(depURL.openStream(), depTemp.toPath(), StandardCopyOption.REPLACE_EXISTING);
			if (depTemp.length() == 0) {
				continue;
			}
			// Copy temp dependency into master local
			JarFile jarFile = new JarFile(depTemp);
			Enumeration<JarEntry> jarEntries = jarFile.entries();
			while(jarEntries.hasMoreElements()) {
				JarEntry je = jarEntries.nextElement();
				String name = je.getName();
				// Skip existing paths
				if (depLocalNames.contains(name))
					continue;
				// Skip self-blacklisted items
				if (dependency.equals(selfURL) &&
						blacklist.stream().anyMatch(b -> name.startsWith(b)))
					continue;
				// write to master
				jos.putNextEntry(je);
				try (InputStream is = jarFile.getInputStream(je)) {
					byte[] data = Streams.from(is);
					jos.write(data, 0, data.length);
				}
				depLocalNames.add(name);
				jos.closeEntry();
			}
			jarFile.close();
			depTemp.delete();
		}
		jos.close();
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
	private static void executeUpdated(String file, String[] args) throws IOException {
		String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		File currentJar = new File(file);
		// Build process: java -jar recaf.jar -args
		List<String> procArgs = new ArrayList<>();
		procArgs.add(javaBin);
		procArgs.add("-jar");
		procArgs.add(currentJar.getPath());
		Collections.addAll(procArgs, args);
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

	private static String getOSType() {
		String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		if (s.contains("win")) {
			return "win";
		}
		if (s.contains("mac") || s.contains("osx")) {
			return "mac";
		}
		return "linux";
	}

	private static void err(String title, String msg) {
		System.err.println(msg);
		JOptionPane.showMessageDialog(null, msg, title, JOptionPane.ERROR_MESSAGE);
	}
}
