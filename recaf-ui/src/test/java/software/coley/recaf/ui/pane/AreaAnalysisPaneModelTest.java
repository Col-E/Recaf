package software.coley.recaf.ui.pane;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.analysis.structure.AreaAnalysisResult;
import software.coley.recaf.services.analysis.structure.AreaFormationKind;
import software.coley.recaf.services.analysis.structure.AreaGroup;
import software.coley.recaf.services.analysis.structure.AreaLink;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.workspace.model.Workspace;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static software.coley.recaf.test.TestClassUtils.createEmptyClass;
import static software.coley.recaf.test.TestClassUtils.fromClasses;

/**
 * Tests for {@link AreaAnalysisPaneModel}.
 */
class AreaAnalysisPaneModelTest extends TestBase {
	private static Workspace workspace;
	private static AreaGroup group1;
	private static AreaGroup group2;
	private static AreaGroup group3;
	private static AreaAnalysisResult result;

	@BeforeAll
	static void setupWorkspace() {
		workspace = TestClassUtils.fromBundle(fromClasses(
				createEmptyClass("a/A"),
				createEmptyClass("a/B"),
				createEmptyClass("b/C"),
				createEmptyClass("c/D"),
				createEmptyClass("c/E"),
				createEmptyClass("d/F"),
				createEmptyClass("e/G"),
				createEmptyClass("f/H")
		));

		group1 = new AreaGroup(1,
				List.of(path("a/A"), path("a/B")),
				AreaFormationKind.SCC,
				"NETWORKING",
				0.82,
				1,
				1,
				false);
		group2 = new AreaGroup(2,
				List.of(path("b/C")),
				AreaFormationKind.MERGED,
				"SECURITY",
				0.68,
				1,
				1,
				true);
		group3 = new AreaGroup(3,
				List.of(path("c/D"), path("c/E")),
				AreaFormationKind.SCC,
				"UI",
				0.79,
				1,
				0,
				false);
		result = new AreaAnalysisResult(
				List.of(group1, group2, group3),
				List.of(
						new AreaLink(1, 2, 8, 2),
						new AreaLink(2, 3, 5, 1),
						new AreaLink(3, 1, 4, 1)
				),
				List.of(),
				false,
				5,
				3,
				3
		);
	}

	@Test
	void setResultSelectsFirstGroupAndClasses() {
		AreaAnalysisPaneModel model = new AreaAnalysisPaneModel();
		model.setResult(result);

		assertEquals(group1, model.getSelectedGroup());
		assertEquals(Set.of("a/A", "a/B"), names(model.getGroupClasses()));
	}

	@Test
	void selectingGroupUpdatesInboundAndOutboundLinks() {
		AreaAnalysisPaneModel model = new AreaAnalysisPaneModel();
		model.setResult(result);
		model.selectGroup(group2);

		assertEquals(Set.of("b/C"), names(model.getGroupClasses()));
		AreaAnalysisPaneModel.NeighborhoodGraph neighborhood = model.getNeighborhoodGraph();
		assertEquals(List.of(2, 1, 3), neighborhood.groups().stream().map(AreaGroup::id).toList());
		assertEquals(List.of("1->2", "2->3", "3->1"),
				neighborhood.links().stream().map(link -> link.sourceGroupId() + "->" + link.targetGroupId()).toList());
	}

	@Test
	void selectingGroupsTrackRecentHistoryInMruOrder() {
		AreaAnalysisPaneModel model = new AreaAnalysisPaneModel();
		model.setResult(result);
		model.selectGroup(group2);
		model.selectGroup(group3);
		model.selectGroup(group2);

		assertEquals(group2, model.getSelectedGroup());
		assertEquals(List.of(2, 3, 1), model.getRecentGroupIds());
		assertEquals(0, model.recentSelectionIndex(2));
		assertEquals(1, model.recentSelectionIndex(3));
		assertEquals(2, model.recentSelectionIndex(1));
	}

	@Test
	void preservesSelectedGroupAcrossResultRefresh() {
		AreaAnalysisPaneModel model = new AreaAnalysisPaneModel();
		model.setResult(result);
		model.selectGroup(group3);

		AreaAnalysisResult refreshed = new AreaAnalysisResult(
				List.of(group1, group2, group3),
				result.links(),
				result.ungroupedClasses(),
				result.spaghettiDetected(),
				result.analyzedClassCount(),
				result.groupCount(),
				result.linkCount()
		);
		model.setResult(refreshed);

		assertEquals(group3, model.getSelectedGroup());
		assertEquals(Set.of("c/D", "c/E"), names(model.getGroupClasses()));
		assertEquals(List.of(3), model.getRecentGroupIds());
	}

	@Test
	void summarySupportSortsLargestGroupsAndFormatsConfidence() {
		List<AreaGroup> sorted = AreaAnalysisSummarySupport.largestGroups(result, 2);

		assertEquals(List.of(group1, group3), sorted);
		assertEquals("82%", AreaAnalysisSummarySupport.formatConfidence(0.82));
		assertEquals("5 classes, 3 groups, 3 links", AreaAnalysisSummarySupport.formatCounts(result));
	}

	@Test
	void recentHistoryCapsAtFiveEntries() {
		AreaGroup group4 = new AreaGroup(4,
				List.of(path("d/F")),
				AreaFormationKind.SCC,
				"UTIL",
				0.75,
				0,
				0,
				false);
		AreaGroup group5 = new AreaGroup(5,
				List.of(path("e/G")),
				AreaFormationKind.SCC,
				"IO",
				0.75,
				0,
				0,
				false);
		AreaGroup group6 = new AreaGroup(6,
				List.of(path("f/H")),
				AreaFormationKind.SCC,
				"MISC",
				0.75,
				0,
				0,
				false);

		AreaAnalysisPaneModel model = new AreaAnalysisPaneModel();
		model.setResult(new AreaAnalysisResult(
				List.of(group1, group2, group3, group4, group5, group6),
				result.links(),
				List.of(),
				false,
				8,
				6,
				3
		));
		model.selectGroup(group2);
		model.selectGroup(group3);
		model.selectGroup(group4);
		model.selectGroup(group5);
		model.selectGroup(group6);

		assertEquals(List.of(6, 5, 4, 3, 2), model.getRecentGroupIds());
		assertEquals(-1, model.recentSelectionIndex(1));
	}

	@Nonnull
	private static ClassPathNode path(String name) {
		ClassPathNode path = workspace.findClass(name);
		assertNotNull(path);
		return path;
	}

	private static Set<String> names(List<ClassPathNode> paths) {
		return paths.stream().map(path -> path.getValue().getName()).collect(Collectors.toSet());
	}
}
