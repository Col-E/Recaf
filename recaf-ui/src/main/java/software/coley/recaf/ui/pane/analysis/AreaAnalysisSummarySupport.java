package software.coley.recaf.ui.pane.analysis;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.analysis.structure.AreaAnalysisResult;
import software.coley.recaf.services.analysis.structure.AreaGroup;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Shared formatting helpers for area-analysis UI.
 *
 * @author Matt Coley
 */
final class AreaAnalysisSummarySupport {
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.#");
	private static final double LOW_CONFIDENCE_THRESHOLD = 0.70;

	private AreaAnalysisSummarySupport() {}

	@Nonnull
	static List<AreaGroup> largestGroups(@Nonnull AreaAnalysisResult result, int limit) {
		return result.groups().stream()
				.sorted(Comparator
						.comparingInt((AreaGroup group) -> group.classes().size()).reversed()
						.thenComparingInt(AreaGroup::id))
				.limit(limit)
				.toList();
	}

	@Nonnull
	static String formatConfidence(double confidence) {
		return Math.round(confidence * 100) + "%";
	}

	@Nonnull
	static String formatCounts(@Nonnull AreaAnalysisResult result) {
		String text = String.format("%d classes, %d groups, %d links",
				result.analyzedClassCount(),
				result.groupCount(),
				result.linkCount());
		if (result.spaghettiDetected())
			text += ", dominant area detected";
		return text;
	}

	@Nonnull
	static String formatGroupPreview(@Nonnull AreaGroup group) {
		return String.format("#%d - %d classes - %s - %s - %s",
				group.id(),
				group.classes().size(),
				group.purpose(),
				group.formationKind().name(),
				formatConfidence(group.confidence()));
	}

	@Nonnull
	static DistributionSummary summarizeGroupSizes(@Nonnull AreaAnalysisResult result) {
		return summarize(result.groups().stream()
				.map(group -> group.classes().size())
				.toList());
	}

	@Nonnull
	static DistributionSummary summarizeLinkCounts(@Nonnull AreaAnalysisResult result) {
		return summarize(result.groups().stream()
				.map(group -> group.inboundLinkCount() + group.outboundLinkCount())
				.toList());
	}

	@Nonnull
	static ConfidenceSummary summarizeConfidence(@Nonnull AreaAnalysisResult result) {
		List<Double> values = result.groups().stream()
				.map(AreaGroup::confidence)
				.sorted()
				.toList();
		if (values.isEmpty())
			return new ConfidenceSummary(0, 0, 0);

		double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
		double median;
		int size = values.size();
		if ((size & 1) == 1) {
			median = values.get(size / 2);
		} else {
			median = (values.get((size / 2) - 1) + values.get(size / 2)) / 2.0;
		}

		int lowConfidenceCount = (int) values.stream()
				.filter(value -> value < LOW_CONFIDENCE_THRESHOLD)
				.count();
		return new ConfidenceSummary(average, median, lowConfidenceCount);
	}

	@Nonnull
	static PurposeSummary summarizePurposes(@Nonnull AreaAnalysisResult result) {
		if (result.groups().isEmpty())
			return new PurposeSummary("None", "None", 0);

		Map<String, Integer> frequencies = new HashMap<>();
		for (AreaGroup group : result.groups())
			frequencies.merge(group.purpose(), 1, Integer::sum);

		List<Map.Entry<String, Integer>> ranked = frequencies.entrySet().stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
						.thenComparing(Map.Entry::getKey))
				.toList();
		String topPurpose = ranked.getFirst().getKey();
		String secondPurpose = ranked.size() > 1 ? ranked.get(1).getKey() : "None";
		return new PurposeSummary(topPurpose, secondPurpose, frequencies.size());
	}

	@Nonnull
	static String formatMetric(double value) {
		return DECIMAL_FORMAT.format(value);
	}

	@Nonnull
	private static DistributionSummary summarize(@Nonnull List<Integer> values) {
		if (values.isEmpty())
			return new DistributionSummary(0, 0, 0);

		List<Integer> sorted = new ArrayList<>(values);
		sorted.sort(Integer::compareTo);

		double average = sorted.stream().mapToInt(Integer::intValue).average().orElse(0);
		double median;
		int size = sorted.size();
		if ((size & 1) == 1) {
			median = sorted.get(size / 2);
		} else {
			median = (sorted.get((size / 2) - 1) + sorted.get(size / 2)) / 2.0;
		}

		Map<Integer, Integer> frequencies = new TreeMap<>();
		for (int value : sorted)
			frequencies.merge(value, 1, Integer::sum);

		int mode = sorted.getFirst();
		int modeFrequency = -1;
		for (Map.Entry<Integer, Integer> entry : frequencies.entrySet()) {
			if (entry.getValue() > modeFrequency) {
				mode = entry.getKey();
				modeFrequency = entry.getValue();
			}
		}

		return new DistributionSummary(average, median, mode);
	}

	record DistributionSummary(double average, double median, int mode) {}

	record ConfidenceSummary(double average, double median, int lowConfidenceCount) {}

	record PurposeSummary(@Nonnull String topPurpose, @Nonnull String secondPurpose, int distinctPurposeCount) {}
}
