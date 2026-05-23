package software.coley.recaf.services.analysis.android;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.FilePathNode;

/**
 * Android permission entry discovered in a manifest.
 *
 * @param manifestPath
 * 		Path to the manifest file where this permission was found.
 * @param permission
 * 		The permission string that was found.
 * @param elementName
 * 		The name of the element that declared the permission.
 * @param attributeName
 * 		The name of the attribute that declared the permission.
 *
 * @author Matt Coley
 */
public record AndroidPermissionEntry(@Nonnull FilePathNode manifestPath,
                                     @Nonnull String permission,
                                     @Nonnull String elementName,
                                     @Nonnull String attributeName) {}
