package software.coley.recaf.services.analysis.android;

import jakarta.annotation.Nonnull;

/**
 * Android permission protection level information.
 *
 * @param baseLevel
 * 		Primary protection level, such as {@code dangerous} or {@code normal}.
 * @param rawLevel
 * 		Raw level value from the permission metadata.
 *
 * @author Matt Coley
 */
public record AndroidPermissionLevel(@Nonnull String baseLevel,
                                     @Nonnull String rawLevel) {}
