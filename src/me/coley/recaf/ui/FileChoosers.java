package me.coley.recaf.ui;

import java.io.File;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import me.coley.event.Bus;
import me.coley.recaf.Logging;
import me.coley.recaf.event.*;
import me.coley.recaf.util.Lang;

public class FileChoosers {
	private static final FileChooser open = new FileChooser();
	private static final FileChooser export = new FileChooser();

	public static void open() {
		File file = open.showOpenDialog(null);
		if (file != null) {
			NewInputEvent.call(file);
		}
	}

	public static void export() {
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
		String dir = System.getProperty("user.dir");
		File fileDir = new File(dir);
		ExtensionFilter filter = new ExtensionFilter(Lang.get("ui.fileprompt.open.extensions"), "*.jar", "*.class");
		open.setInitialDirectory(fileDir);
		open.setTitle(Lang.get("ui.filepropt.open"));
		open.getExtensionFilters().add(filter);
		open.setSelectedExtensionFilter(filter);
		export.setInitialDirectory(fileDir);
		export.setTitle(Lang.get("ui.filepropt.export"));
		export.getExtensionFilters().add(filter);
		export.setSelectedExtensionFilter(filter);
	}
}
