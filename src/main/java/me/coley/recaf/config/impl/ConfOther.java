package me.coley.recaf.config.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.config.Conf;
import me.coley.recaf.config.Config;
import me.coley.recaf.event.ExportEvent;
import me.coley.recaf.event.NewInputEvent;

/**
 * Options for miscellaneous things.
 * 
 * @author Matt
 */
public class ConfOther extends Config {
	/**
	 * Working / current directory.
	 */
	private final static String CURRENT_DIR = System.getProperty("user.dir");
	/**
	 * Recent file paths.
	 */
	@Conf(category = "other", key = "recentfiles", hide = true)
	public List<String> recent = new ArrayList<>();
	/**
	 * Number of recent files allowed to be stored in {@link #recent}.
	 */
	@Conf(category = "other", key = "maxrecentfiles")
	public int maxRecentFiles = 6;
	/**
	 * Recent path used by the open dialog.
	 */
	@Conf(category = "other", key = "recentimport", hide = true)
	public String recentImport = CURRENT_DIR;
	/**
	 * Recent path used by the export dialog.
	 */
	@Conf(category = "other", key = "recentexport", hide = true)
	public String recentExport = CURRENT_DIR;

	public ConfOther() {
		super("rc_other");
		load();
		// Listen to events to update values
		Bus.subscribe(this);
		// In case recentImport is null for some reason.
		if (recentImport == null && recent.size() > 0) {
			recentImport = recent.get(0);
		}
	}

	/**
	 * @return Directory to use for import file-chooser. Based on the most
	 *         recent file opened.
	 */
	public File getRecentImportDir() {
		File recent = new File(recentImport);
		if (recent.exists()) {
			if (recent.isDirectory()) 
				return recent;
			File parent = recent.getParentFile();
			if (parent != null) return parent;
		}
		return new File(CURRENT_DIR);
	}

	/**
	 * @return Directory to use for export file-chooser. Based on the most
	 *         recent file exported.
	 */
	public File getRecentExportDir() {
		File recent = new File(recentExport);
		if (recent.exists()) {
			if (recent.isDirectory()) 
				return recent;
			File parent = recent.getParentFile();
			if (parent != null) return parent;
		}
		return new File(CURRENT_DIR);
	}

	/**
	 * Listen to input events to update the recently imported list / item.
	 */
	@Listener
	private void onImport(NewInputEvent input) {
		File file = input.get().input;
		if (file != null) {
			recentImport = file.getAbsolutePath();
			recent.add(0, recentImport);
			while (recent.size() > maxRecentFiles) {
				recent.remove(maxRecentFiles - 1);
			}
		}
		cleanRecent();
	}

	/**
	 * Prune duplicate entries.
	 */
	private void cleanRecent() {
		for (String key : new HashSet<>(recent)) {
			int first = recent.indexOf(key);
			int last = recent.lastIndexOf(key);
			if (first != last) {
				recent.remove(last);
			}
		}
	}

	/**
	 * Listen to export events to update the recently exported item.
	 */
	@Listener
	private void onExport(ExportEvent input) {
		File file = input.getFile();
		recentExport = file.getAbsolutePath();
	}
	
	@Override
	protected JsonValue convert(Class<?> type, Object value) {
		if (type.isAssignableFrom(List.class)) {
			return Json.array(recent.toArray(new String[0]));
		}
		return null;
	}

	@Override
	protected Object parse(Class<?> type, JsonValue value) {
		if (type.isAssignableFrom(List.class)) {
			if (value.isArray()) {
				recent.clear();
				value.asArray().forEach(v -> {
					recent.add(v.asString());
				});
				cleanRecent();
			}
		}
		return null;
	}

	/**
	 * Static getter.
	 * 
	 * @return ConfDisplay instance.
	 */
	public static ConfOther instance() {
		return ConfOther.instance(ConfOther.class);
	}
}
