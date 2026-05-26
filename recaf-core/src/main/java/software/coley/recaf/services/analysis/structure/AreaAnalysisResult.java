package software.coley.recaf.services.analysis.structure;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.ClassPathNode;

import java.util.List;

/**
 * Result of application area analysis.
 *
 * @param groups
 * 		Discovered groups ordered deterministically.
 * @param links
 * 		Directed links between groups ordered by source then target identifier.
 * @param ungroupedClasses
 * 		Classes skipped from grouping.
 * @param spaghettiDetected
 * 		Flag indicating a dominant area consumed most analyzed classes.
 * @param analyzedClassCount
 * 		Number of analyzed classes included in grouping.
 * @param groupCount
 * 		Number of resulting groups.
 * @param linkCount
 * 		Number of resulting inter-group links.
 *
 * @author Matt Coley
 */
public record AreaAnalysisResult(@Nonnull List<AreaGroup> groups,
                                 @Nonnull List<AreaLink> links,
                                 @Nonnull List<ClassPathNode> ungroupedClasses,
                                 boolean spaghettiDetected,
                                 int analyzedClassCount,
                                 int groupCount,
                                 int linkCount) {}
