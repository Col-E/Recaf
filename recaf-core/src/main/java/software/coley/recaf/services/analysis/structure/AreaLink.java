package software.coley.recaf.services.analysis.structure;

/**
 * Directed relationship between two analyzed areas.
 *
 * @param sourceGroupId
 * 		Source group identifier.
 * @param targetGroupId
 * 		Target group identifier.
 * @param weight
 * 		Aggregated edge weight between groups.
 * @param edgeCount
 * 		Number of contributing dependency edges between groups.
 *
 * @author Matt Coley
 */
public record AreaLink(int sourceGroupId, int targetGroupId, int weight, int edgeCount) {}
