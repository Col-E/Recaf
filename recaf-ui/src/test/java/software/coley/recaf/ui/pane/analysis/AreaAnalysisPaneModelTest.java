package software.coley.recaf.ui.pane.analysis;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.analysis.structure.AreaAnalysisResult;
import software.coley.recaf.services.analysis.structure.AreaFormationKind;
import software.coley.recaf.services.analysis.structure.AreaGroup;
import software.coley.recaf.services.analysis.structure.AreaLink;
import software.coley.recaf.test.TestBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.coley.recaf.test.TestClassUtils.createEmptyClass;

class AreaAnalysisPaneModelTest extends TestBase {
	@Test
	void setResultStartsInOverviewWithoutSelection() {
		AreaAnalysisPaneModel model = new AreaAnalysisPaneModel();
		model.setResult(sampleResult());

		assertEquals(AreaAnalysisPaneModel.AreaViewMode.OVERVIEW, model.getViewMode());
		assertNull(model.getSelectedGroup());
		assertTrue(model.getDisplayedClasses().isEmpty());
		assertEquals(Set.of(1, 3, 2), model.getExplorerGroups().stream().map(AreaGroup::id).collect(Collectors.toSet()));
	}

	@Test
	void selectingGroupBuildsScopedBothGraph() {
		AreaAnalysisPaneModel model = new AreaAnalysisPaneModel();
		AreaAnalysisResult result = sampleResult();
		model.setResult(result);

		AreaGroup selected = result.groups().get(1);
		model.selectGroup(selected);

		assertEquals(AreaAnalysisPaneModel.AreaViewMode.GROUP_FOCUS_BOTH, model.getViewMode());
		assertEquals(selected, model.getSelectedGroup());
		assertEquals(Set.of("b/C"), names(model.getDisplayedClasses()));
		assertEquals(Set.of(2, 1, 3), model.getScopedGraphResult().groups().stream().map(AreaGroup::id).collect(Collectors.toSet()));
		assertEquals(List.of("1->2", "2->3"),
				model.getScopedGraphResult().links().stream().map(link -> link.sourceGroupId() + "->" + link.targetGroupId()).toList());
	}

	@Test
	void searchMatchesPurposeAndClassName() {
		AreaAnalysisPaneModel model = new AreaAnalysisPaneModel();
		AreaAnalysisResult result = sampleResult();
		model.setResult(result);

		model.setSearchQuery("security");
		assertTrue(model.getExplorerGroups().isEmpty());

		model.setSearchQuery("c/e");
		assertEquals(List.of(3), model.getExplorerGroups().stream().map(AreaGroup::id).toList());
	}

	@Test
	void purposeFilterMatchesExactPurpose() {
		AreaAnalysisPaneModel model = new AreaAnalysisPaneModel();
		AreaAnalysisResult result = sampleResult();
		model.setResult(result);

		model.setSelectedPurpose("SECURITY");
		assertEquals(List.of(2), model.getExplorerGroups().stream().map(AreaGroup::id).toList());
	}

	@Test
	void ungroupedSelectionClearsGraphAndShowsClasses() {
		AreaAnalysisPaneModel model = new AreaAnalysisPaneModel();
		AreaAnalysisResult result = new AreaAnalysisResult(
				sampleResult().groups(),
				sampleResult().links(),
				List.of(path("x/U"), path("x/V")),
				false,
				7,
				3,
				3
		);
		model.setResult(result);
		model.showUngrouped();

		assertTrue(model.isUngroupedSelection());
		assertNull(model.getSelectedGroup());
		assertEquals(Set.of("x/U", "x/V"), names(model.getDisplayedClasses()));
		assertTrue(model.getScopedGraphResult().groups().isEmpty());
	}

	@Test
	void bothModeCapsHighDegreeNeighborhoods() {
		int inbound = 20;
		int outbound = 20;
		AreaAnalysisResult result = highDegreeResult(inbound, outbound);
		AreaAnalysisPaneModel model = new AreaAnalysisPaneModel();
		model.setResult(result);
		model.selectGroup(result.groups().getFirst());

		AreaAnalysisPaneModel.ScopedGraphResult scoped = model.getScopedGraphResult();
		assertEquals(25, scoped.groups().size());
		assertEquals(24, scoped.links().size());
		assertEquals(inbound - 12, scoped.hiddenInboundNeighborCount());
		assertEquals(outbound - 12, scoped.hiddenOutboundNeighborCount());
		assertEquals(inbound + outbound - 24, scoped.hiddenEdgeCount());
	}

	@Nonnull
	private static AreaAnalysisResult sampleResult() {
		AreaGroup group1 = new AreaGroup(1,
				List.of(path("a/A"), path("a/B")),
				AreaFormationKind.SCC,
				"NETWORKING",
				0.82,
				1,
				1,
				false);
		AreaGroup group2 = new AreaGroup(2,
				List.of(path("b/C")),
				AreaFormationKind.MERGED,
				"SECURITY",
				0.68,
				1,
				1,
				true);
		AreaGroup group3 = new AreaGroup(3,
				List.of(path("c/D"), path("c/E")),
				AreaFormationKind.SCC,
				"UI",
				0.79,
				1,
				0,
				false);
		return new AreaAnalysisResult(
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

	@Nonnull
	private static AreaAnalysisResult highDegreeResult(int inboundCount, int outboundCount) {
		List<AreaGroup> groups = new ArrayList<>();
		List<AreaLink> links = new ArrayList<>();
		AreaGroup hub = new AreaGroup(1,
				List.of(path("hub/Main")),
				AreaFormationKind.SCC,
				"UTIL",
				0.74,
				inboundCount,
				outboundCount,
				true);
		groups.add(hub);

		int nextId = 2;
		for (int i = 0; i < inboundCount; i++) {
			AreaGroup inbound = new AreaGroup(nextId,
					List.of(path("in/" + nextId)),
					AreaFormationKind.SCC,
					"NETWORKING",
					0.70,
					0,
					1,
					false);
			groups.add(inbound);
			links.add(new AreaLink(inbound.id(), hub.id(), inboundCount - i, 1));
			nextId++;
		}

		for (int i = 0; i < outboundCount; i++) {
			AreaGroup outbound = new AreaGroup(nextId,
					List.of(path("out/" + nextId)),
					AreaFormationKind.SCC,
					"IO",
					0.65,
					1,
					0,
					false);
			groups.add(outbound);
			links.add(new AreaLink(hub.id(), outbound.id(), outboundCount - i, 1));
			nextId++;
		}

		return new AreaAnalysisResult(groups, links, List.of(), false, groups.size(), groups.size(), links.size());
	}

	@Nonnull
	private static ClassPathNode path(String name) {
		return new ClassPathNode(createEmptyClass(name));
	}

	private static Set<String> names(List<ClassPathNode> paths) {
		return paths.stream().map(path -> path.getValue().getName()).collect(Collectors.toSet());
	}
}
