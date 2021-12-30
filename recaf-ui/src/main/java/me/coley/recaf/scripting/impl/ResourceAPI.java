package me.coley.recaf.scripting.impl;

import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceIO;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourceAPI {
    private static final Logger logger = Logging.get(ResourceAPI.class);

    public static Resource createResourceFromPath(String _path, boolean read) {
        Path path = Paths.get(_path);
        try {
            return ResourceIO.fromPath(path, read);
        } catch (IOException e) {
            logger.error("Failed to create resource {}: {}", path.getFileName(), e.getLocalizedMessage());
            return null;
        }
    }

    public static Resource createResourceFromPath(String path) {
        return createResourceFromPath(path, true);
    }
}
