package me.coley.recaf.scripting.impl;

import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
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
 * @author Matt Coley
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
	 *
	 * @return {@code true} when the workspace was updated.
	 * {@code false} when the change was rejected.
	 */
	public static boolean setWorkspace(Workspace workspace) {
		return RecafUI.getController().setWorkspace(workspace);
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
			if (workspace != null)
				workspace.addLibrary(resource);
			else
				logger.warn("Cannot add resource to non-existent workspace!");
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
		if (workspace != null)
			workspace.removeLibrary(resource);
	}

	/**
	 * Removes a resource from the {@link #getWorkspace() current workspace}.
	 *
	 * @param resource
	 * 		Resource to remove.
	 */
	public static void removeResource(Resource resource) {
		Workspace workspace = getWorkspace();
		if (workspace != null)
			workspace.removeLibrary(resource);
	}

	/**
	 * @param workspace
	 * 		Workspace to fetch from.
	 *
	 * @return The primary resource of the workspace.
	 * Contains the classes and files for modification in Recaf.
	 */
	public static Resource getPrimaryResource(Workspace workspace) {
		if (workspace == null)
			return null;
		return workspace.getResources().getPrimary();
	}

	/**
	 * @return The primary resource of the {@link #getWorkspace() current workspace}.
	 * Contains the classes and files for modification in Recaf.
	 */
	public static Resource getPrimaryResource() {
		return getPrimaryResource(getWorkspace());
	}

	/**
	 * @param internalName
	 * 		Internal class name.
	 *
	 * @return Info wrapper of the class, or {@code null} if no class by the name exists in the primary resource.
	 */
	public static ClassInfo getPrimaryClassInfo(String internalName) {
		return getClassInfo(getPrimaryResource(), internalName);
	}

	/**
	 * @param internalName
	 * 		Internal dex class name.
	 *
	 * @return Info wrapper of the class, or {@code null} if no dex class by the name exists in the primary resource.
	 */
	public static DexClassInfo getPrimaryDexClassInfo(String internalName) {
		return getDexClassInfo(getPrimaryResource(), internalName);
	}

	/**
	 * @param path
	 * 		File path.
	 *
	 * @return Info wrapper of the file, or {@code null} if no file by the path exists in the primary resource.
	 */
	public static FileInfo getPrimaryFileInfo(String path) {
		return getFileInfo(getPrimaryResource(), path);
	}

	/**
	 * @param resource
	 * 		Resource to pull from.
	 * @param internalName
	 * 		Internal class name.
	 *
	 * @return Info wrapper of the class, or {@code null} if no class by the name exists in the resource.
	 */
	public static ClassInfo getClassInfo(Resource resource, String internalName) {
		if (resource == null)
			return null;
		return resource.getClasses().get(internalName);
	}

	/**
	 * @param resource
	 * 		Resource to pull from.
	 * @param internalName
	 * 		Internal dex class name.
	 *
	 * @return Info wrapper of the dex class, or {@code null} if no dex class by the name exists in the resource.
	 */
	public static DexClassInfo getDexClassInfo(Resource resource, String internalName) {
		if (resource == null)
			return null;
		return resource.getDexClasses().get(internalName);
	}

	/**
	 * @param resource
	 * 		Resource to pull from.
	 * @param path
	 * 		File path.
	 *
	 * @return Info wrapper of the file, or {@code null} if no file by the path exists in the resource.
	 */
	public static FileInfo getFileInfo(Resource resource, String path) {
		if (resource == null)
			return null;
		return resource.getFiles().get(path);
	}

	/**
	 * @param content
	 * 		Class bytecode to place into primary resource.
	 */
	public static void putPrimaryClassInfo(byte[] content) {
		putClassInfo(getPrimaryResource(), content);
	}

	/**
	 * @param info
	 * 		Class to place into primary resource.
	 */
	public static void putPrimaryClassInfo(ClassInfo info) {
		putClassInfo(getPrimaryResource(), info);
	}

	/**
	 * @param resource
	 * 		Resource to put the class into.
	 * @param content
	 * 		Class bytecode to place into resource.
	 */
	public static void putClassInfo(Resource resource, byte[] content) {
		if (resource != null)
			resource.getClasses().put(ClassInfo.read(content));
	}

	/**
	 * @param resource
	 * 		Resource to put the class into.
	 * @param info
	 * 		Class to place into resource.
	 */
	public static void putClassInfo(Resource resource, ClassInfo info) {
		if (resource != null)
			resource.getClasses().put(info);
	}

	/**
	 * @param info
	 * 		Dex class to place into primary resource.
	 */
	public static void putPrimaryDexClassInfo(DexClassInfo info) {
		putDexClassInfo(getPrimaryResource(), info);
	}

	/**
	 * @param resource
	 * 		Resource to put the class into.
	 * @param info
	 * 		Dex class to place into resource.
	 */
	public static void putDexClassInfo(Resource resource, DexClassInfo info) {
		if (resource != null) {
			String dexName = info.getDexPath();
			String className = info.getName();
			resource.getDexClasses().put(dexName, className, info);
		}
	}

	/**
	 * @param path
	 * 		File path.
	 * @param content
	 * 		File to place into primary resource.
	 */
	public static void putPrimaryFileInfo(String path, byte[] content) {
		putPrimaryFileInfo(new FileInfo(path, content));
	}

	/**
	 * @param info
	 * 		File to place into primary resource.
	 */
	public static void putPrimaryFileInfo(FileInfo info) {
		putFileInfo(getPrimaryResource(), info);
	}

	/**
	 * @param resource
	 * 		Resource to put the file into.
	 * @param path
	 * 		File path.
	 * @param content
	 * 		File to place into resource.
	 */
	public static void putFileInfo(Resource resource, String path, byte[] content) {
		putFileInfo(resource, new FileInfo(path, content));
	}

	/**
	 * @param resource
	 * 		Resource to put the file into.
	 * @param info
	 * 		File to place into resource.
	 */
	public static void putFileInfo(Resource resource, FileInfo info) {
		if (resource != null)
			resource.getFiles().put(info);
	}
}
