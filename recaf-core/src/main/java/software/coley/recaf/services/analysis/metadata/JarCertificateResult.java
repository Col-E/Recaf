package software.coley.recaf.services.analysis.metadata;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.path.FilePathNode;

import java.security.cert.Certificate;
import java.util.List;

/**
 * Parsed certificate data from a jar signature block file.
 *
 * @param filePath
 * 		Path to the signature block file that was parsed.
 * @param certificates
 * 		List of certificates that were parsed from the signature block file.
 * 		If the file was not a valid signature block, this will be empty.
 * @param parseError
 * 		If parsing the signature block failed, any error that occurred doing so.
 *
 * @author Matt Coley
 */
public record JarCertificateResult(@Nonnull FilePathNode filePath,
                                   @Nonnull List<Certificate> certificates,
                                   @Nullable String parseError) {}
