package software.coley.recaf.services.analysis.structure;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.ClassPathNode;

import java.util.List;

/**
 * Group of related classes forming a logical application area.
 *
 * @param id
 * 		Stable identifier assigned by analysis order.
 * @param classes
 * 		Classes in the group, sorted by internal name.
 * @param formationKind
 * 		How the group was formed.
 * @param purpose
 * 		Rough estimate of the group's dominant purpose.
 * @param confidence
 * 		Confidence score in the range {@code [0, 1]}.
 * @param inboundLinkCount
 * 		Number of incoming links from other groups.
 * @param outboundLinkCount
 * 		Number of outgoing links to other groups.
 * @param containsEntryPoint
 * 		Flag indicating whether the group contains a discovered entry point class.
 *
 * @author Matt Coley
 */
public record AreaGroup(int id,
                        @Nonnull List<ClassPathNode> classes,
                        @Nonnull AreaFormationKind formationKind,
                        @Nonnull String purpose,
                        double confidence,
                        int inboundLinkCount,
                        int outboundLinkCount,
                        boolean containsEntryPoint) {}
