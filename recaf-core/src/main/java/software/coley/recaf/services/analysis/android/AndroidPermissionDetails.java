package software.coley.recaf.services.analysis.android;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Android permission entry with resolved protection levels.
 *
 * @param entry
 * 		The discovered permission entry.
 * @param levels
 * 		Resolved protection levels for the permission.
 *
 * @author Matt Coley
 */
public record AndroidPermissionDetails(@Nonnull AndroidPermissionEntry entry,
                                       @Nonnull List<AndroidPermissionLevel> levels) {}
