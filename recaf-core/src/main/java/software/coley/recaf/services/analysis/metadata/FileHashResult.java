package software.coley.recaf.services.analysis.metadata;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.FilePathNode;

import java.util.EnumMap;

/**
 * Hash values for a file resource.
 *
 * @param filePath
 * 		Path to the file that was hashed.
 * @param hashes
 * 		Map of hash algorithm to the resulting hash value.
 *
 * @author Matt Coley
 */
public record FileHashResult(@Nonnull FilePathNode filePath,
                             @Nonnull EnumMap<HashAlgorithm, String> hashes) {}
