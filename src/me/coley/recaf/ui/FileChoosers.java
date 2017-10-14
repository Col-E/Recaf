package me.coley.recaf.ui;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileChoosers {
	private JFileChooser fileChooser;

	/**
	 * @return The file chooser. If it is null it is instantiated and set to the
	 * working directory with a filter for jar files.
	 */
	public JFileChooser getFileChooser() {
		return getFileChooser("Java Archives", "jar");
	}

	/**
	 * @return The file chooser. If it is null it is instantiated and set to the
	 * working directory with a filter for the given file type. To allow any
	 * type, have the parameters be null.
	 *
	 * @param fileType Name of the type of file
	 * @param extension Actual file extension.
	 */
	public JFileChooser getFileChooser(String fileType, String extension) {
		if (fileChooser == null) {
			fileChooser = new JFileChooser();
			String dir = System.getProperty("user.dir");
			File fileDir = new File(dir);
			fileChooser.setDialogTitle("Open File");
			fileChooser.setCurrentDirectory(fileDir);
		}
		if (fileType == null || extension == null) {
			fileChooser.setFileFilter(null);
		} else {
			FileNameExtensionFilter filter = new FileNameExtensionFilter(fileType, extension);
			fileChooser.setFileFilter(filter);
		}
		return fileChooser;
	}

	/**
	 * Creates and returns a file chooser set in the working directory.
	 *
	 * @return The file chooser.
	 */
	public JFileChooser createFileSaver() {
		JFileChooser fileSaver = new JFileChooser();
		String dir = System.getProperty("user.dir");
		File fileDir = new File(dir);
		fileSaver.setCurrentDirectory(fileDir);
		fileSaver.setDialogTitle("Save to File");
		return fileSaver;
	}
}
