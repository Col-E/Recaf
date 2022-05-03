package me.coley.recaf.ui.prompt;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import me.coley.recaf.ExportUtil;
import me.coley.recaf.RecafUI;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.DialogConfig;
import me.coley.recaf.mapping.Mappings;
import me.coley.recaf.ui.control.tree.WorkspaceTreeWrapper;
import me.coley.recaf.ui.pane.WorkspacePane;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The ugly file choosing logic gets tossed in here.
 *
 * @author Matt Coley
 */
public class WorkspaceIOPrompts {
	private static final FileChooser fcLoad = new FileChooser();
	private static final FileChooser fcExport = new FileChooser();
	private static final FileChooser fcMappingIn = new FileChooser();
	private static final FileChooser fcMappingOut = new FileChooser();

	static {
		FileChooser.ExtensionFilter any = new FileChooser.ExtensionFilter(Lang.get("dialog.filefilter.any"), "*");
		FileChooser.ExtensionFilter mappings = new FileChooser.ExtensionFilter(Lang.get("dialog.filefilter.mapping"),
				"*.txt", "*.map", "*.mapping", "*.enigma", "*.pro", "*.srg", "*.tsrg", "*.tiny", "*.tinyv2");
		FileChooser.ExtensionFilter mappingOutput =
				new FileChooser.ExtensionFilter(Lang.get("dialog.filefilter.mapping"),
						"*.txt", "*.map");
		FileChooser.ExtensionFilter applicationOrWorkspace =
				new FileChooser.ExtensionFilter(Lang.get("dialog.filefilter.input"),
						"*.jar", "*.war", "*.zip", "*.apk", "*.class", "*.json");

		fcLoad.titleProperty().bind(Lang.getBinding("dialog.file.open"));
		fcLoad.getExtensionFilters().add(applicationOrWorkspace);
		fcLoad.getExtensionFilters().add(any);
		fcLoad.setSelectedExtensionFilter(applicationOrWorkspace);

		fcExport.titleProperty().bind(Lang.getBinding("dialog.file.export"));
		fcExport.getExtensionFilters().add(applicationOrWorkspace);
		fcExport.getExtensionFilters().add(any);
		fcExport.setSelectedExtensionFilter(applicationOrWorkspace);

		fcMappingIn.titleProperty().bind(Lang.getBinding("dialog.file.open"));
		fcMappingIn.getExtensionFilters().add(mappings);
		fcMappingIn.getExtensionFilters().add(any);
		fcMappingIn.setSelectedExtensionFilter(mappings);

		fcMappingOut.titleProperty().bind(Lang.getBinding("dialog.file.export"));
		fcMappingOut.getExtensionFilters().add(mappingOutput);
		fcMappingOut.getExtensionFilters().add(any);
		fcMappingOut.setSelectedExtensionFilter(mappings);
	}

	/**
	 * Reads location data from the {@link DialogConfig} instance.
	 */
	public static void setupLocations() {
		DialogConfig config = config();
		initLocation(fcLoad, config.appLoadLocation);
		initLocation(fcExport, config.appExportLocation);
		initLocation(fcMappingIn, config.mapLoadLocation);
		initLocation(fcMappingOut, config.mapExportLocation);
	}

	/**
	 * @param chooser
	 * 		Chooser to set initial location of.
	 * @param location
	 * 		Initial location.
	 */
	private static void initLocation(FileChooser chooser, String location) {
		// Sanity check
		if (location == null)
			location = System.getProperty("user.dir");
		// Ensure location exists before setting
		File file = new File(location);
		if (!file.isDirectory())
			file = new File(System.getProperty("user.dir"));
		chooser.setInitialDirectory(file);
	}

	/**
	 * Opens a prompt to export the current workspace's primary resource.
	 */
	public static void promptExportApplication() {
		initLocation(fcExport, config().appExportLocation);
		File saveLocation = fcExport.showSaveDialog(parent());
		if (saveLocation != null) {
			config().appExportLocation = getParent(saveLocation);
			ExportUtil.write(saveLocation.toPath());
		}
	}

	/**
	 * Opens a prompt to select multiple files to create a new workspace with.
	 *
	 * @return Selected files.
	 */
	public static List<Path> promptWorkspaceFiles() {
		initLocation(fcLoad, config().appLoadLocation);
		List<File> files = fcLoad.showOpenMultipleDialog(parent());
		List<Path> paths;
		if (files == null || files.isEmpty()) {
			paths = Collections.emptyList();
		} else {
			config().appLoadLocation = getParent(files.get(0));
			paths = files.stream()
					.map(File::toPath)
					.collect(Collectors.toList());
		}
		return paths;
	}

	/**
	 * Opens a prompt to open the given mappings file.
	 *
	 * @return Mapping text or {@code null} if cancelled/failed to read.
	 */
	public static String promptMappingInput() {
		initLocation(fcMappingIn, config().mapLoadLocation);
		File file = fcMappingIn.showOpenDialog(parent());
		if (file == null) {
			return null;
		}
		try {
			config().mapLoadLocation = getParent(file);
			return new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Opens a prompt to export the given mappings to a file.
	 *
	 * @param mappings
	 * 		Mappings instance.
	 */
	public static void promptMappingExport(Mappings mappings) {
		initLocation(fcMappingOut, config().mapExportLocation);
		File saveLocation = fcMappingOut.showSaveDialog(parent());
		if (saveLocation != null) {
			config().mapExportLocation = getParent(saveLocation);
			String text = mappings.exportText();
			try {
				Files.write(saveLocation.toPath(), text.getBytes());
			} catch (IOException ex) {
				// TODO: Shouldn't happen, but log error / show warning
			}
		}
	}


	/**
	 * Convenience call to {@link #handleFiles(List, WorkspaceActionType)} with no default action.
	 *
	 * @param files
	 * 		Paths to read from.
	 */
	public static void handleFiles(List<Path> files) {
		handleFiles(files, null);
	}

	/**
	 * @param files
	 * 		Paths to read from.
	 * @param overrideAction
	 * 		Default action to take with files once loaded.
	 */
	public static void handleFiles(List<Path> files, WorkspaceActionType overrideAction) {
		// Update overlay
		WorkspaceTreeWrapper wrapper = WorkspacePane.getInstance().getTree();
		wrapper.addLoadingOverlay(files);
		// Read files, this is the slow part that is why we run this on a separate thread
		List<Resource> resources = WorkspaceDropPrompts.readResources(files);
		// Check for initial case when no workspace is open
		if (wrapper.getWorkspace() == null) {
			FxThreadUtil.run(() -> {
				Workspace created = WorkspaceDropPrompts.createWorkspace(resources);
				if (created != null) {
					RecafUI.getController().setWorkspace(created);
				}
			});
			// Remove overlay
			wrapper.clearOverlay();
			return;
		}
		FxThreadUtil.run(() -> {
			// Check what the user wants to do with these files
			WorkspaceAction result;
			if (overrideAction == null) {
				switch (Configs.display().onFileDrop) {
					default:
					case CHOOSE:
						result = WorkspaceDropPrompts.prompt(resources);
						break;
					case CREATE_NEW:
						result = WorkspaceDropPrompts.workspace(WorkspaceDropPrompts.createWorkspace(resources));
						break;
					case ADD_LIBRARY:
						result = WorkspaceDropPrompts.add(resources);
						break;
				}
			} else {
				result = overrideAction.createResult(resources);
			}
			switch (result.getAction()) {
				case ADD_TO_WORKSPACE:
					// Users chose to add files to workspace as library resources
					result.getLibraries().forEach(library -> wrapper.getWorkspace().addLibrary(library));
					break;
				case CREATE_NEW_WORKSPACE:
					// Users chose to make new workspace from dropped file(s)
					RecafUI.getController().setWorkspace(result.getWorkspace());
					break;
				case CANCEL:
				default:
					// Users chose to cancel
			}
		});
		// Remove overlay
		wrapper.clearOverlay();
	}

	private static String getParent(File file) {
		return file.getAbsoluteFile().getParent();
	}

	private static DialogConfig config() {
		return Configs.dialogs();
	}

	private static Window parent() {
		return RecafUI.getWindows().getMainWindow();
	}
}
