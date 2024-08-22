package software.coley.recaf.services.workspace.patch.model;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.PathNode;

/**
 * Outlines the removal of a given path.
 *
 * @param path
 * 		Path to the class/file to remove.
 *
 * @author Matt Coley
 */
public record RemovePath(@Nonnull PathNode<?> path) {}
