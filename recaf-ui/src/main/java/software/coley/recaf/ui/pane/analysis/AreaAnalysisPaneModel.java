package software.coley.recaf.ui.pane.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.analysis.structure.AreaAnalysisResult;
import software.coley.recaf.services.analysis.structure.AreaGroup;
import software.coley.recaf.services.analysis.structure.AreaLink;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Backing state for {@link AreaAnalysisPane}.
 *
 * @author Matt Coley
 */
class AreaAnalysisPaneModel {
	static final int OVERVIEW_SECTION_LIMIT = 5;
	static final int LARGE_GROUP_THRESHOLD = 10;
	static final int HIGH_LINK_THRESHOLD = 5;
	static final int GRAPH_GROUP_BUDGET = 25;
	static final int GRAPH_EDGE_BUDGET = 48;
	static final int GRAPH_DIRECTION_SOFT_CAP = 12;

	private static final Comparator<AreaGroup> EXPLORER_ORDER = Comparator
			.comparingInt((AreaGroup group) -> group.classes().size()).reversed()
			.thenComparing(Comparator.comparingInt((AreaGroup group) -> group.inboundLinkCount() + group.outboundLinkCount()).reversed())
			.thenComparing(Comparator.comparingInt(AreaGroup::inboundLinkCount).reversed())
			.thenComparing(Comparator.comparingInt(AreaGroup::outboundLinkCount).reversed())
			.thenComparingInt(AreaGroup::id);
	private static final Comparator<AreaGroup> LARGEST_ORDER = Comparator
			.comparingInt((AreaGroup group) -> group.classes().size()).reversed()
			.thenComparingInt(AreaGroup::id);
	private static final Comparator<AreaGroup> HIGH_FAN_IN_ORDER = Comparator
			.comparingInt(AreaGroup::inboundLinkCount).reversed()
			.thenComparing(Comparator.comparingInt((AreaGroup group) -> group.classes().size()).reversed())
			.thenComparingInt(AreaGroup::id);
	private static final Comparator<AreaGroup> HIGH_FAN_OUT_ORDER = Comparator
			.comparingInt(AreaGroup::outboundLinkCount).reversed()
			.thenComparing(Comparator.comparingInt((AreaGroup group) -> group.classes().size()).reversed())
			.thenComparingInt(AreaGroup::id);
	private static final Comparator<AreaLink> INBOUND_LINK_ORDER = Comparator
			.comparingInt(AreaLink::weight).reversed()
			.thenComparingInt(AreaLink::edgeCount).reversed()
			.thenComparingInt(AreaLink::sourceGroupId);
	private static final Comparator<AreaLink> OUTBOUND_LINK_ORDER = Comparator
			.comparingInt(AreaLink::weight).reversed()
			.thenComparingInt(AreaLink::edgeCount).reversed()
			.thenComparingInt(AreaLink::targetGroupId);

	private final ObservableList<AreaGroup> explorerGroups = FXCollections.observableArrayList();
	private final ObservableList<ClassPathNode> displayedClasses = FXCollections.observableArrayList();
	private final ReadOnlyObjectWrapper<AreaAnalysisResult> result = new ReadOnlyObjectWrapper<>();
	private final ReadOnlyObjectWrapper<AreaGroup> selectedGroup = new ReadOnlyObjectWrapper<>();
	private final ReadOnlyObjectWrapper<EdgeSelection> selectedEdge = new ReadOnlyObjectWrapper<>();
	private final ReadOnlyObjectWrapper<AreaViewMode> viewMode = new ReadOnlyObjectWrapper<>(AreaViewMode.OVERVIEW);
	private final ReadOnlyObjectWrapper<ScopedGraphResult> scopedGraph = new ReadOnlyObjectWrapper<>(ScopedGraphResult.empty());
	private final ReadOnlyBooleanWrapper ungroupedSelection = new ReadOnlyBooleanWrapper(false);
	private final Map<Integer, AreaGroup> groupsById = new TreeMap<>();
	private final Map<Integer, List<AreaLink>> inboundLinksByGroup = new TreeMap<>();
	private final Map<Integer, List<AreaLink>> outboundLinksByGroup = new TreeMap<>();
	private String searchQuery = "";
	private boolean entryPointsOnly;
	private boolean largeGroupsOnly;
	private boolean highFanInOnly;
	private boolean highFanOutOnly;
	private String selectedPurpose;

	@Nonnull
	ObservableList<AreaGroup> getExplorerGroups() {
		return explorerGroups;
	}

	@Nonnull
	ObservableList<ClassPathNode> getDisplayedClasses() {
		return displayedClasses;
	}

	@Nonnull
	ReadOnlyObjectProperty<AreaAnalysisResult> resultProperty() {
		return result.getReadOnlyProperty();
	}

	@Nullable
	AreaAnalysisResult getResult() {
		return result.get();
	}

	@Nonnull
	ReadOnlyObjectProperty<AreaGroup> selectedGroupProperty() {
		return selectedGroup.getReadOnlyProperty();
	}

	@Nullable
	AreaGroup getSelectedGroup() {
		return selectedGroup.get();
	}

	@Nonnull
	ReadOnlyObjectProperty<EdgeSelection> selectedEdgeProperty() {
		return selectedEdge.getReadOnlyProperty();
	}

	@Nullable
	EdgeSelection getSelectedEdge() {
		return selectedEdge.get();
	}

	@Nonnull
	ReadOnlyObjectProperty<AreaViewMode> viewModeProperty() {
		return viewMode.getReadOnlyProperty();
	}

	@Nonnull
	AreaViewMode getViewMode() {
		return viewMode.get();
	}

	@Nonnull
	ReadOnlyObjectProperty<ScopedGraphResult> scopedGraphProperty() {
		return scopedGraph.getReadOnlyProperty();
	}

	@Nonnull
	ScopedGraphResult getScopedGraphResult() {
		return scopedGraph.get();
	}

	@Nonnull
	ReadOnlyBooleanProperty ungroupedSelectionProperty() {
		return ungroupedSelection.getReadOnlyProperty();
	}

	boolean isUngroupedSelection() {
		return ungroupedSelection.get();
	}

	boolean isEntryPointsOnly() {
		return entryPointsOnly;
	}

	boolean isLargeGroupsOnly() {
		return largeGroupsOnly;
	}

	boolean isHighFanInOnly() {
		return highFanInOnly;
	}

	boolean isHighFanOutOnly() {
		return highFanOutOnly;
	}

	@Nullable
	String getSelectedPurpose() {
		return selectedPurpose;
	}

	@Nonnull
	String getSearchQuery() {
		return searchQuery;
	}

	boolean hasActiveDiscoveryFilters() {
		return !searchQuery.isBlank() || entryPointsOnly || largeGroupsOnly || highFanInOnly || highFanOutOnly || selectedPurpose != null;
	}

	void clear() {
		result.set(null);
		explorerGroups.clear();
		displayedClasses.clear();
		groupsById.clear();
		inboundLinksByGroup.clear();
		outboundLinksByGroup.clear();
		selectedGroup.set(null);
		selectedEdge.set(null);
		viewMode.set(AreaViewMode.OVERVIEW);
		scopedGraph.set(ScopedGraphResult.empty());
		ungroupedSelection.set(false);
	}

	void setResult(@Nonnull AreaAnalysisResult analysisResult) {
		result.set(analysisResult);
		groupsById.clear();
		inboundLinksByGroup.clear();
		outboundLinksByGroup.clear();
		for (AreaGroup group : analysisResult.groups()) {
			groupsById.put(group.id(), group);
			inboundLinksByGroup.put(group.id(), new ArrayList<>());
			outboundLinksByGroup.put(group.id(), new ArrayList<>());
		}
		for (AreaLink link : analysisResult.links()) {
			inboundLinksByGroup.computeIfAbsent(link.targetGroupId(), ignored -> new ArrayList<>()).add(link);
			outboundLinksByGroup.computeIfAbsent(link.sourceGroupId(), ignored -> new ArrayList<>()).add(link);
		}
		inboundLinksByGroup.values().forEach(list -> list.sort(INBOUND_LINK_ORDER));
		outboundLinksByGroup.values().forEach(list -> list.sort(OUTBOUND_LINK_ORDER));

		selectedGroup.set(null);
		selectedEdge.set(null);
		viewMode.set(AreaViewMode.OVERVIEW);
		ungroupedSelection.set(false);
		displayedClasses.clear();
		refreshDerivedState();
	}

	void selectGroup(@Nullable AreaGroup group) {
		selectedEdge.set(null);
		ungroupedSelection.set(false);
		selectedGroup.set(group);
		if (group == null) {
			displayedClasses.clear();
			viewMode.set(AreaViewMode.OVERVIEW);
		} else {
			displayedClasses.setAll(group.classes());
			if (!viewMode.get().isGraphFocus())
				viewMode.set(AreaViewMode.GROUP_FOCUS_BOTH);
		}
		refreshScopedGraph();
	}

	void showOverview() {
		selectedEdge.set(null);
		selectedGroup.set(null);
		ungroupedSelection.set(false);
		displayedClasses.clear();
		viewMode.set(AreaViewMode.OVERVIEW);
		refreshScopedGraph();
	}

	void showUngrouped() {
		AreaAnalysisResult analysisResult = result.get();
		selectedEdge.set(null);
		selectedGroup.set(null);
		viewMode.set(AreaViewMode.OVERVIEW);
		ungroupedSelection.set(true);
		displayedClasses.setAll(analysisResult == null ? List.of() : analysisResult.ungroupedClasses());
		refreshScopedGraph();
	}

	void selectEdge(@Nullable EdgeSelection edgeSelection) {
		selectedEdge.set(edgeSelection);
	}

	void setViewMode(@Nonnull AreaViewMode nextMode) {
		if (!nextMode.isGraphFocus()) {
			viewMode.set(AreaViewMode.OVERVIEW);
		} else if (selectedGroup.get() != null) {
			viewMode.set(nextMode);
			ungroupedSelection.set(false);
		}
		selectedEdge.set(null);
		refreshScopedGraph();
	}

	void setSearchQuery(@Nullable String query) {
		String nextQuery = query == null ? "" : query.trim();
		if (Objects.equals(searchQuery, nextQuery))
			return;
		searchQuery = nextQuery;
		refreshDerivedState();
	}

	void setEntryPointsOnly(boolean entryPointsOnly) {
		if (this.entryPointsOnly == entryPointsOnly)
			return;
		this.entryPointsOnly = entryPointsOnly;
		refreshDerivedState();
	}

	void setLargeGroupsOnly(boolean largeGroupsOnly) {
		if (this.largeGroupsOnly == largeGroupsOnly)
			return;
		this.largeGroupsOnly = largeGroupsOnly;
		refreshDerivedState();
	}

	void setHighFanInOnly(boolean highFanInOnly) {
		if (this.highFanInOnly == highFanInOnly)
			return;
		this.highFanInOnly = highFanInOnly;
		refreshDerivedState();
	}

	void setHighFanOutOnly(boolean highFanOutOnly) {
		if (this.highFanOutOnly == highFanOutOnly)
			return;
		this.highFanOutOnly = highFanOutOnly;
		refreshDerivedState();
	}

	void setSelectedPurpose(@Nullable String selectedPurpose) {
		if (Objects.equals(this.selectedPurpose, selectedPurpose))
			return;
		this.selectedPurpose = selectedPurpose;
		refreshDerivedState();
	}

	@Nullable
	AreaGroup findGroup(int groupId) {
		return groupsById.get(groupId);
	}

	@Nonnull
	List<AreaGroup> getLargestGroups() {
		return topGroups(LARGEST_ORDER, OVERVIEW_SECTION_LIMIT, group -> true);
	}

	@Nonnull
	List<AreaGroup> getEntryPointGroups() {
		return topGroups(LARGEST_ORDER, OVERVIEW_SECTION_LIMIT, AreaGroup::containsEntryPoint);
	}

	@Nonnull
	List<AreaGroup> getHighFanInGroups() {
		return topGroups(HIGH_FAN_IN_ORDER, OVERVIEW_SECTION_LIMIT, group -> group.inboundLinkCount() > 0);
	}

	@Nonnull
	List<AreaGroup> getHighFanOutGroups() {
		return topGroups(HIGH_FAN_OUT_ORDER, OVERVIEW_SECTION_LIMIT, group -> group.outboundLinkCount() > 0);
	}

	@Nonnull
	List<ClassPathNode> getUngroupedPreviewClasses(int limit) {
		AreaAnalysisResult analysisResult = result.get();
		if (analysisResult == null || analysisResult.ungroupedClasses().isEmpty())
			return List.of();
		return analysisResult.ungroupedClasses().stream().limit(limit).toList();
	}

	@Nonnull
	List<AreaLink> getInboundLinks(@Nonnull AreaGroup group) {
		return List.copyOf(inboundLinksByGroup.getOrDefault(group.id(), List.of()));
	}

	@Nonnull
	List<AreaLink> getOutboundLinks(@Nonnull AreaGroup group) {
		return List.copyOf(outboundLinksByGroup.getOrDefault(group.id(), List.of()));
	}

	private void refreshDerivedState() {
		explorerGroups.setAll(filteredGroups().stream()
				.sorted(EXPLORER_ORDER)
				.toList());
		refreshScopedGraph();
	}

	private void refreshScopedGraph() {
		AreaGroup group = selectedGroup.get();
		AreaAnalysisResult analysisResult = result.get();
		AreaViewMode mode = viewMode.get();
		if (group == null || analysisResult == null || !mode.isGraphFocus()) {
			scopedGraph.set(ScopedGraphResult.empty());
			return;
		}

		List<AreaLink> inbound = inboundLinksByGroup.getOrDefault(group.id(), List.of());
		List<AreaLink> outbound = outboundLinksByGroup.getOrDefault(group.id(), List.of());
		List<AreaLink> visibleLinks = new ArrayList<>();
		Map<Integer, AreaGroup> visibleGroups = new TreeMap<>();
		visibleGroups.put(group.id(), group);

		switch (mode) {
			case GROUP_FOCUS_INBOUND -> addLinks(inbound, true, visibleLinks, visibleGroups, GRAPH_GROUP_BUDGET - 1, GRAPH_EDGE_BUDGET);
			case GROUP_FOCUS_OUTBOUND -> addLinks(outbound, false, visibleLinks, visibleGroups, GRAPH_GROUP_BUDGET - 1, GRAPH_EDGE_BUDGET);
			case GROUP_FOCUS_BOTH -> {
				addLinks(inbound.stream().limit(GRAPH_DIRECTION_SOFT_CAP).toList(), true, visibleLinks, visibleGroups, GRAPH_DIRECTION_SOFT_CAP, GRAPH_DIRECTION_SOFT_CAP);
				addLinks(outbound.stream().limit(GRAPH_DIRECTION_SOFT_CAP).toList(), false, visibleLinks, visibleGroups, GRAPH_DIRECTION_SOFT_CAP, GRAPH_DIRECTION_SOFT_CAP);
			}
			case OVERVIEW -> {
				scopedGraph.set(ScopedGraphResult.empty());
				return;
			}
		}

		long visibleInboundCount = visibleLinks.stream().filter(link -> link.targetGroupId() == group.id()).count();
		long visibleOutboundCount = visibleLinks.stream().filter(link -> link.sourceGroupId() == group.id()).count();
		int hiddenInboundNeighbors = (int) inbound.stream().filter(link -> !visibleGroups.containsKey(link.sourceGroupId())).count();
		int hiddenOutboundNeighbors = (int) outbound.stream().filter(link -> !visibleGroups.containsKey(link.targetGroupId())).count();

		int candidateEdgeCount = switch (mode) {
			case GROUP_FOCUS_INBOUND -> inbound.size();
			case GROUP_FOCUS_OUTBOUND -> outbound.size();
			case GROUP_FOCUS_BOTH -> inbound.size() + outbound.size();
			case OVERVIEW -> 0;
		};
		int hiddenEdgeCount = Math.max(0, candidateEdgeCount - visibleLinks.size());
		scopedGraph.set(new ScopedGraphResult(
				group,
				mode,
				List.copyOf(visibleGroups.values()),
				List.copyOf(visibleLinks),
				(int) visibleInboundCount,
				(int) visibleOutboundCount,
				hiddenInboundNeighbors,
				hiddenOutboundNeighbors,
				hiddenEdgeCount
		));
	}

	private void addLinks(@Nonnull List<AreaLink> candidates,
	                      boolean inbound,
	                      @Nonnull List<AreaLink> visibleLinks,
	                      @Nonnull Map<Integer, AreaGroup> visibleGroups,
	                      int maxNewGroups,
	                      int maxAdditionalEdges) {
		if (maxNewGroups <= 0 || maxAdditionalEdges <= 0)
			return;

		int newGroupsAdded = 0;
		int startEdgeCount = visibleLinks.size();
		for (AreaLink link : candidates) {
			if (visibleLinks.size() >= GRAPH_EDGE_BUDGET || visibleLinks.size() - startEdgeCount >= maxAdditionalEdges)
				break;

			int neighborId = inbound ? link.sourceGroupId() : link.targetGroupId();
			AreaGroup neighbor = groupsById.get(neighborId);
			if (neighbor == null)
				continue;

			if (!visibleGroups.containsKey(neighborId)) {
				if (visibleGroups.size() >= GRAPH_GROUP_BUDGET || newGroupsAdded >= maxNewGroups)
					break;
				visibleGroups.put(neighborId, neighbor);
				newGroupsAdded++;
			}

			visibleLinks.add(link);
		}
	}

	@Nonnull
	private List<AreaGroup> topGroups(@Nonnull Comparator<AreaGroup> comparator, int limit, @Nonnull Predicate<AreaGroup> extraFilter) {
		return filteredGroups().stream()
				.filter(extraFilter)
				.sorted(comparator)
				.limit(limit)
				.toList();
	}

	@Nonnull
	private List<AreaGroup> filteredGroups() {
		AreaAnalysisResult analysisResult = result.get();
		if (analysisResult == null)
			return List.of();
		return analysisResult.groups().stream()
				.filter(this::matchesFilters)
				.toList();
	}

	private boolean matchesFilters(@Nonnull AreaGroup group) {
		if (entryPointsOnly && !group.containsEntryPoint())
			return false;
		if (largeGroupsOnly && group.classes().size() < LARGE_GROUP_THRESHOLD)
			return false;
		if (highFanInOnly && group.inboundLinkCount() < HIGH_LINK_THRESHOLD)
			return false;
		if (highFanOutOnly && group.outboundLinkCount() < HIGH_LINK_THRESHOLD)
			return false;
		if (selectedPurpose != null && !selectedPurpose.equals(group.purpose()))
			return false;
		if (!searchQuery.isBlank() && !matchesSearch(group))
			return false;
		return true;
	}

	private boolean matchesSearch(@Nonnull AreaGroup group) {
		String normalizedQuery = searchQuery.toLowerCase(Locale.ROOT);
		if (String.valueOf(group.id()).contains(normalizedQuery))
			return true;
		for (ClassPathNode classPath : group.classes()) {
			if (classPath.getValue().getName().toLowerCase(Locale.ROOT).contains(normalizedQuery))
				return true;
		}
		return false;
	}

	enum AreaViewMode {
		OVERVIEW,
		GROUP_FOCUS_INBOUND,
		GROUP_FOCUS_OUTBOUND,
		GROUP_FOCUS_BOTH;

		boolean isGraphFocus() {
			return this != OVERVIEW;
		}
	}

	record EdgeSelection(@Nonnull AreaGroup sourceGroup, @Nonnull AreaGroup targetGroup, @Nonnull AreaLink link) {}

	record ScopedGraphResult(@Nullable AreaGroup selectedGroup,
	                         @Nonnull AreaViewMode mode,
	                         @Nonnull List<AreaGroup> groups,
	                         @Nonnull List<AreaLink> links,
	                         int visibleInboundCount,
	                         int visibleOutboundCount,
	                         int hiddenInboundNeighborCount,
	                         int hiddenOutboundNeighborCount,
	                         int hiddenEdgeCount) {
		private static final ScopedGraphResult EMPTY = new ScopedGraphResult(null, AreaViewMode.OVERVIEW, List.of(), List.of(), 0, 0, 0, 0, 0);

		@Nonnull
		static ScopedGraphResult empty() {
			return EMPTY;
		}
	}
}
