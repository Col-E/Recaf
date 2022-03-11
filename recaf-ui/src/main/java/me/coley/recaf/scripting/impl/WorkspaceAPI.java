package me.coley.recaf.scripting.impl;

import me.coley.recaf.RecafUI;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceIO;
import me.coley.recaf.workspace.resource.Resources;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Utility functions for working with workspaces.
 *
 * @author Wolfie / win32kbase
 */
public class WorkspaceAPI {
	private static final Logger logger = Logging.get(WorkspaceAPI.class);

	/**
	 * @param resources
	 * 		Resources to build workspace with.
	 *
	 * @return Workspace containing the given resources.
	 */
	public static Workspace createWorkspace(Resources resources) {
		return new Workspace(resources);
	}

	/**
	 * @param resource
	 * 		Resource to build workspace with.
	 *
	 * @return Workspace containing the given resource.
	 */
	public static Workspace createWorkspace(Resource resource) {
		return createWorkspace(new Resources(resource));
	}

	/**
	 * @param resource
	 * 		File path to build workspace with.
	 *
	 * @return Workspace containing the given file's contents.
	 */
	public static Workspace createWorkspace(Path resource) {
		return createWorkspace(resource.toFile());
	}

	/**
	 * @param resource
	 * 		File path to build workspace with.
	 *
	 * @return Workspace containing the given file's contents.
	 */
	public static Workspace createWorkspace(File resource) {
		return createWorkspace(new Resources(ResourceAPI.createResource(resource)));
	}

	/**
	 * @param workspace
	 * 		Workspace to apply to the current Recaf instance.
	 */
	public static void setWorkspace(Workspace workspace) {
		RecafUI.getController().setWorkspace(workspace);
	}

	/**
	 * @return Current workspace in Recaf.
	 */
	public static Workspace getWorkspace() {
		return RecafUI.getController().getWorkspace();
	}

	/**
	 * @param workspace
	 * 		Workspace to modify.
	 * @param file
	 * 		Path to file to add to workspace.
	 *
	 * @return Loaded rsource from the path.
	 */
	public static Resource addResource(Workspace workspace, File file) {
		return addResource(workspace, file.toPath());
	}

	/**
	 * @param workspace
	 * 		Workspace to modify.
	 * @param path
	 * 		Path to file to add to workspace.
	 *
	 * @return Loaded rsource from the path.
	 */
	public static Resource addResource(Workspace workspace, Path path) {
		try {
			Resource resource = ResourceIO.fromPath(path, true);
			workspace.addLibrary(resource);
			return resource;
		} catch (IOException e) {
			logger.error("Script failed to add file to workspace: {}", path);
			return null;
		}
	}


	/**
	 * Adds a resource to the {@link #getWorkspace() current workspace}.
	 *
	 * @param file
	 * 		Path to file to add to workspace.
	 *
	 * @return Loaded resource from the path.
	 */
	public static Resource addResource(File file) {
		return addResource(getWorkspace(), file.toPath());
	}

	/**
	 * Adds a resource to the {@link #getWorkspace() current workspace}.
	 *
	 * @param path
	 * 		Path to file to add to workspace.
	 *
	 * @return Loaded resource from the path.
	 */
	public static Resource addResource(Path path) {
		return addResource(getWorkspace(), path);
	}

	/**
	 * Removes a resource from the given workspace.
	 *
	 * @param workspace
	 * 		Workspace to modify.
	 * @param resource
	 * 		Resource to remove.
	 */
	public static void removeResource(Workspace workspace, Resource resource) {
		workspace.removeLibrary(resource);
	}

	/**
	 * Removes a resource from the {@link #getWorkspace() current workspace}.
	 *
	 * @param resource
	 * 		Resource to remove.
	 */
	public static void removeResource(Resource resource) {
		getWorkspace().removeLibrary(resource);
	}

	/**
	 * @param workspace
	 * 		Workspace to fetch from.
	 *
	 * @return The primary resource of the workspace.
	 * Contains the classes and files for modification in Recaf.
	 */
	public static Resource getPrimaryResource(Workspace workspace) {
		return workspace.getResources().getPrimary();
	}

	/**
	 * @return The primary resource of the {@link #getWorkspace() current workspace}.
	 * Contains the classes and files for modification in Recaf.
	 */
	public static Resource getPrimaryResource() {
		return getPrimaryResource(getWorkspace());
	}
}
