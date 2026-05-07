package software.coley.recaf.services.mapping.matching;

import jakarta.annotation.Nonnull;

/**
 * Pairing of similar members between two matched classes.
 *
 * @param sourceName
 * 		Source member name.
 * @param sourceDescriptor
 * 		Source member descriptor.
 * @param targetName
 * 		Target member name.
 * @param targetDescriptor
 * 		Target member descriptor.
 * @param similarity
 * 		Pairing similarity in {@code [0.0, 1.0]}.
 *
 * @author Matt Coley
 */
public record SimilarityMemberMatch(@Nonnull String sourceName,
                                    @Nonnull String sourceDescriptor,
                                    @Nonnull String targetName,
                                    @Nonnull String targetDescriptor,
                                    double similarity) {
	public boolean isField() {
		return sourceDescriptor.charAt(0) != '(';
	}

	public boolean isMethod() {
		return sourceDescriptor.charAt(0) == '(';
	}
}
