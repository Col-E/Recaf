package software.coley.recaf.services.analysis.antitamper;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.ClassPathNode;

import java.util.List;

/**
 * Anti-reversal result for illegal names.
 *
 * @author Matt Coley
 */
public record IllegalNameAnalysis(@Nonnull List<ClassPathNode> classesWithIllegalNames) implements AntiReversalAnalysisResult {}
