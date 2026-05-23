package software.coley.recaf.services.analysis.metadata;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.FilePathNode;

import java.util.List;

/**
 * Jar signing metadata report.
 *
 * @param manifestPath
 * 		Path to the manifest file of the jar.
 * @param signatureFilePaths
 * 		Paths to the signature files found in the jar.
 * @param certificateResults
 * 		Results of parsing the signature files for certificates.
 *
 * @author Matt Coley
 */
public record JarSigningReport(@Nonnull FilePathNode manifestPath,
                               @Nonnull List<FilePathNode> signatureFilePaths,
                               @Nonnull List<JarCertificateResult> certificateResults) {}
