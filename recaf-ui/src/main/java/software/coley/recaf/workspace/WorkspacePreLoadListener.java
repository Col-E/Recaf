package software.coley.recaf.workspace;

import jakarta.annotation.Nonnull;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.util.io.ByteSource;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

/**
 * Listener for when a user has selected some input to load as a workspace.
 * This is triggered when they:
 * <ul>
 *     <li>Press 'finish' on the workspace open wizard</li>
 *     <li>Press an entry in the recent workspace menu</li>
 *     <li>Drag and drop one or more files into Recaf</li>
 * </ul>
 * Handles input as {@link Path} items since most other importable data types
 * are already in memory like {@link ByteSource} and others like {@link URL}
 * get locally fetched, meaning the temp file can be accessed as a {@link Path}
 * anyways.
 *
 * @author Matt Coley
 */
public interface WorkspacePreLoadListener extends PrioritySortable {
	/**
	 * @param primaryPath
	 * 		Path to the primary resource file.
	 * @param supportingPaths
	 * 		Paths to the supporting resource files.
	 */
	void onPreLoad(@Nonnull Path primaryPath, @Nonnull List<Path> supportingPaths);
}
