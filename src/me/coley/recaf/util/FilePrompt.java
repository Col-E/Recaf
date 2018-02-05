package me.coley.recaf.util;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import me.coley.recaf.ui.Lang;

public class FilePrompt {
	private static JFileChooser load, save;

	/**
	 * @return The file chooser. If it is null it is instantiated and set to the
	 *         working directory with a filter for jar files.
	 */
	public static JFileChooser getLoader() {
		return getLoader(Lang.get("fileprompt.javafilter"), "jar", "class");
	}

	/**
	 * @return The file chooser. If it is null it is instantiated and set to the
	 *         working directory with a filter for the given file type. To allow
	 *         any type, have the parameters be null.
	 *
	 * @param fileType
	 *            Name of the type of file.
	 * @param extension
	 *            Actual file extension.
	 */
	public static JFileChooser getLoader(String fileType, String... extension) {
		if (load == null) {
			load = new JFileChooser();
			String dir = System.getProperty("user.dir");
			File fileDir = new File(dir);
			load.setDialogTitle(Lang.get("fileprompt.open"));
			load.setCurrentDirectory(fileDir);
			if (fileType == null || extension.length == 0) {
				load.setFileFilter(null);
			} else {
				FileNameExtensionFilter filter = new FileNameExtensionFilter(fileType, extension);
				load.setFileFilter(filter);
			}
			load.setAcceptAllFileFilterUsed(false);
		}
		return load;
	}

	/**
	 * @return The file chooser. If it is null it is instantiated and set to the
	 *         working directory with a filter for jar files.
	 */
	public static JFileChooser getSaver() {
		return getLoader(Lang.get("fileprompt.javafilter"), "jar", "class");
	}
	/**
	 * Creates and returns a file chooser set in the working directory.
	 *
	 * @return The file chooser.
	 */
	public static JFileChooser getSaver(String fileType, String extension) {
		if (save == null) {
			save = new JFileChooser();
			String dir = System.getProperty("user.dir");
			File fileDir = new File(dir);
			save.setDialogTitle(Lang.get("fileprompt.save"));
			save.setCurrentDirectory(fileDir);
			if (fileType == null || extension == null) {
				save.setFileFilter(null);
			} else {
				FileNameExtensionFilter filter = new FileNameExtensionFilter(fileType, extension);
				save.setFileFilter(filter);
			}
		}
		return save;
	}
}