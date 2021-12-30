package me.coley.recaf.scripting.impl;

import me.coley.recaf.Recaf;
import me.coley.recaf.RecafUI;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceIO;
import me.coley.recaf.workspace.resource.Resources;
import org.slf4j.Logger;

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

    public static void setPrimaryWorkspace(Workspace workspace) {
        RecafUI.getController().setWorkspace(workspace);
    }

    public static Workspace getCurrentWorkspace() {
        return RecafUI.getController().getWorkspace();
    }

    public static Resource addFile(Workspace workspace, String path) {
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

    public static Resource getPrimaryResource(Workspace workspace) {
        return workspace.getResources().getPrimary();
    }

    public static Resource getPrimaryResource() {
        return getCurrentWorkspace().getResources().getPrimary();
    }

    public static Resource addFile(String path) {
        return addFile(getCurrentWorkspace(), path);
    }

    public static Logger getLogger() {
        return logger;
    }
}
