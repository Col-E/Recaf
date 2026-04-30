package software.coley.recaf.services.phantom.model;

import jakarta.annotation.Nonnull;

/**
 * Required inner-class metadata for a generated phantom class.
 *
 * @param innerName
 * 		Inner class internal name.
 * @param innerSimpleName
 * 		Simple name stored in the inner-class attribute.
 *
 * @author Matt Coley
 */
public record PhantomInnerRequirement(@Nonnull String innerName, @Nonnull String innerSimpleName) {}
