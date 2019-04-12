package me.coley.recaf.ui;

import java.io.File;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import me.coley.event.Bus;
import me.coley.recaf.Logging;
import me.coley.recaf.config.impl.ConfOther;
import me.coley.recaf.event.*;
import me.coley.recaf.util.Lang;

public class FileChoosers {
	private static final FileChooser open = new FileChooser();
	private static final FileChooser export = new FileChooser();

	public static void open() {
		// Save location
		File dir = ConfOther.instance().getRecentImportDir();
		if (dir != null) open.setInitialDirectory(dir);
		// Open file and invoke new input event
		File file = open.showOpenDialog(null);
		if (file != null) {
			NewInputEvent.call(file);
		}
	}

	public static void export() {
		// Save location
		File dir = ConfOther.instance().getRecentExportDir();
		if (dir != null) export.setInitialDirectory(dir);
		// Open file and invoke save event
		File file = export.showSaveDialog(null);
		if (file != null) {
			try {
				Bus.post(new RequestExportEvent(file));
			} catch (Exception e) {
				Logging.error(e);
			}
		}
	}

	static {
		ExtensionFilter filter = new ExtensionFilter(Lang.get("ui.fileprompt.open.extensions"), "*.jar", "*.class");
		open.setTitle(Lang.get("ui.filepropt.open"));
		open.getExtensionFilters().add(filter);
		open.setSelectedExtensionFilter(filter);
		export.setTitle(Lang.get("ui.filepropt.export"));
		export.getExtensionFilters().add(filter);
		export.setSelectedExtensionFilter(filter);
	}
}
