package me.coley.recaf.config;

import me.coley.recaf.util.IOUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * Private configuration that are intended for user-access.
 *
 * @author Matt
 */
public class ConfBackend extends Config {
	private static final String CURRENT_DIR = System.getProperty("user.dir");
	/**
	 * Recently opened files <i>(from primary resources)</i> - absolute paths.
	 */
	@Conf("backend.recents")
	public List<String> recentFiles = new ArrayList<>();
	/**
	 * Recent path used by the load dialog.
	 */
	@Conf("backend.recentopen")
	public String recentLoad = CURRENT_DIR;
	/**
	 * Recent path used by the save-application dialog.
	 */
	@Conf("backend.recentsave.app")
	public String recentSaveApp = CURRENT_DIR;
	/**
	 * Recent path used by the save-workspace dialog.
	 */
	@Conf("backend.recentsave.workspace")
	public String recentSaveWorkspace = CURRENT_DIR;
	/**
	 * Recent path used by the save-map dialog.
	 */
	@Conf("backend.recentsave.map")
	public String recentSaveMap = CURRENT_DIR;
	/**
	 * Check to determine if user should be told to read the documentation.
	 */
	@Conf("backend.firsttime")
	public boolean firstTime = true;
	/**
	 * Enable compression on output.
	 */
	@Conf("backend.compressexport")
	public boolean compress = true;

	ConfBackend() {
		super("backend");
	}

	/**
	 * @return Copy of recently opened files. {@link #recentFiles}.
	 */
	public List<String> getRecentFiles() {
		return new ArrayList<>(recentFiles);
	}

	@Override
	public void onLoad() {
		// Remove duplicates
		Set<String> temp = new LinkedHashSet<>(recentFiles);
		recentFiles.clear();
		recentFiles.addAll(temp);
	}

	/**
	 * Update {@link #getRecentLoadDir() recent load directory} and {@link #getRecentFiles()
	 * recent files list}.
	 *
	 * @param path
	 * 		Path loaded.
	 * @param maxRecentFiles
	 * 		Maximum recent files to support.
	 */
	public void onLoad(Path path, int maxRecentFiles) {
		String stringPath = IOUtil.toString(path);
		recentLoad = stringPath;
		// Update path in list
		recentFiles.remove(stringPath);
		recentFiles.add(0, stringPath);
		// Prune list if it hits max size
		if(recentFiles.size() > maxRecentFiles)
			recentFiles.remove(recentFiles.size() - 1);
	}

	/**
	 * @return Directory to use for import file-chooser.
	 * Based on the most recent file opened.
	 */
	public File getRecentLoadDir() {
		return getDir(recentLoad);
	}

	/**
	 * @return Directory to use for save-application file-chooser.
	 * Based on the most recent file exported.
	 */
	public File getRecentSaveAppDir() {
		return getDir(recentSaveApp);
	}

	/**
	 * @return Directory to use for save-workspace file-chooser.
	 * Based on the most recent file exported.
	 */
	public File getRecentSaveWorkspaceDir() {
		return getDir(recentSaveWorkspace);
	}

	/**
	 * @return Directory to use for save-map file-chooser.
	 * Based on the most recent file exported.
	 */
	public File getRecentSaveMapDir() {
		return getDir(recentSaveMap);
	}

	private File getDir(String root) {
		File recent = new File(root);
		if(recent.exists()) {
			if(recent.isDirectory())
				return recent;
			File parent = recent.getParentFile();
			if(parent != null)
				return parent;
		}
		return new File(CURRENT_DIR);
	}

}
