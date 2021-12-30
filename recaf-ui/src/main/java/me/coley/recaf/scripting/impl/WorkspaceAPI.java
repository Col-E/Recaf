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
import java.nio.file.Paths;

public class WorkspaceAPI {
    private static final Logger logger = Logging.get(WorkspaceAPI.class);

    public static Workspace createWorkspace(Resources resources) {
        return new Workspace(resources);
    }

    public static Workspace createWorkspace(Resource resource) {
        return createWorkspace(new Resources(resource));
    }

    public static Workspace createWorkspace(File resource) {
        return createWorkspace(new Resources(ResourceAPI.createResource(resource)));
    }

    public static void setPrimaryWorkspace(Workspace workspace) {
        RecafUI.getController().setWorkspace(workspace);
    }

    public static Workspace getPrimaryWorkspace() {
        return RecafUI.getController().getWorkspace();
    }

    public static Resource addResource(Workspace workspace, String path) {
        try {
            Resource resource = ResourceIO.fromPath(Paths.get(path), true);
            workspace.addLibrary(resource);
            return resource;
        }
        catch (IOException e) {
            logger.error("Script failed to add file to workspace: {}", path);
            return null;
        }
    }

    public static Resource addResource(Workspace workspace, File file) {
        return addResource(workspace, file.getPath());
    }

    public static Resource addResource(File file) {
        return addResource(getPrimaryWorkspace(), file.getPath());
    }

    public static Resource addResource(String path) {
        return addResource(getPrimaryWorkspace(), path);
    }

    public static void removeResource(Workspace workspace, Resource resource) {
        workspace.removeLibrary(resource);
    }

    public static void removeResource(Resource resource) {
        getPrimaryWorkspace().removeLibrary(resource);
    }

    public static Resource getPrimaryResource(Workspace workspace) {
        return workspace.getResources().getPrimary();
    }

    public static Resource getPrimaryResource() {
        return getPrimaryWorkspace().getResources().getPrimary();
    }

    public static Logger getLogger() {
        return logger;
    }
}
