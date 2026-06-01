package software.coley.recaf.services.info.summary.builtin;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.services.analysis.android.AndroidAnalysisService;
import software.coley.recaf.services.analysis.android.AndroidPermissionDetails;
import software.coley.recaf.services.analysis.android.AndroidPermissionLevel;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.Batch;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Summarizer that shows requested permissions from an Android application.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AndroidPermissionSummarizer implements ResourceSummarizer {
	private static final List<String> LEVEL_ORDER = List.of("dangerous", "signature", "internal", "normal", "unknown");
	private final AndroidAnalysisService androidAnalysisService;
	private final CellConfigurationService cellConfigurationService;

	@Inject
	public AndroidPermissionSummarizer(@Nonnull AndroidAnalysisService androidAnalysisService,
	                                   @Nonnull CellConfigurationService cellConfigurationService) {
		this.androidAnalysisService = androidAnalysisService;
		this.cellConfigurationService = cellConfigurationService;
	}

	@Override
	public boolean summarize(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull SummaryConsumer consumer) {
		List<AndroidPermissionDetails> permissionDetails = androidAnalysisService.findRequestedPermissionDetails(workspace, resource);
		if (permissionDetails.isEmpty())
			return false;

		Batch batch = FxThreadUtil.batch();
		batch.add(() -> {
			Label title = new BoundLabel(Lang.getBinding("service.analysis.permissions"));
			title.getStyleClass().addAll(Styles.TITLE_4);
			consumer.appendSummary(title);
		});

		Map<FilePathNode, Map<String, Collection<PermissionDisplay>>> groupedPermissions = groupPermissions(permissionDetails);
		if (groupedPermissions.size() > 1) {
			// Multiple manifests, show a pane for each.
			for (AndroidPermissionDetails permissionDetail : permissionDetails) {
				FilePathNode manifestPath = permissionDetail.entry().manifestPath();
				Map<String, Collection<PermissionDisplay>> manifestPermissions = groupedPermissions.remove(manifestPath);
				if (manifestPermissions != null)
					batch.add(() -> consumer.appendSummary(createManifestPane(permissionDetail, manifestPermissions, true)));
			}
		} else {
			// Single manifest, no need to show the manifest path in the UI.
			Map<String, Collection<PermissionDisplay>> manifestPermissions = groupedPermissions.values().iterator().next();
			AndroidPermissionDetails permissionDetail = permissionDetails.getFirst();
			batch.add(() -> consumer.appendSummary(createManifestPane(permissionDetail, manifestPermissions, false)));
		}
		batch.execute();
		return true;
	}

	@Nonnull
	private Map<FilePathNode, Map<String, Collection<PermissionDisplay>>> groupPermissions(@Nonnull List<AndroidPermissionDetails> permissionDetails) {
		// Paths are instance-unique.
		Map<FilePathNode, Map<String, Collection<PermissionDisplay>>> groupedPermissions = new IdentityHashMap<>();
		for (AndroidPermissionDetails permissionDetail : permissionDetails) {
			FilePathNode manifestPath = permissionDetail.entry().manifestPath();

			// Group permissions by protection level, then group those groups by manifest path.
			Map<String, Collection<PermissionDisplay>> levelGroups = groupedPermissions.computeIfAbsent(manifestPath, ignored -> new TreeMap<>());
			for (AndroidPermissionLevel level : permissionDetail.levels())
				// Permissions in this group are sorted by name.
				levelGroups.computeIfAbsent(level.baseLevel(), ignored -> new TreeSet<>())
						.add(new PermissionDisplay(permissionDetail.entry().permission(), level));
		}
		return groupedPermissions;
	}

	@Nonnull
	private Node createManifestPane(@Nonnull AndroidPermissionDetails permissionDetail,
	                                @Nonnull Map<String, Collection<PermissionDisplay>> levelGroups,
	                                boolean showHeader) {
		VBox content = new VBox(10);
		for (String levelName : orderedLevelNames(levelGroups)) {
			VBox section = new VBox(4);
			Label heading = new Label(levelName);
			heading.getStyleClass().add(Styles.TEXT_BOLD);
			section.getChildren().add(heading);
			for (PermissionDisplay permission : levelGroups.get(levelName))
				section.getChildren().add(createPermissionRow(permission));
			content.getChildren().add(section);
		}

		// If there are multiple manifests, we want to show the containing resource for them to differentiate.
		if (showHeader) {
			ResourcePathNode manifestParentResource = permissionDetail.entry()
					.manifestPath()
					.getPathOfType(WorkspaceResource.class);
			Label header = new Label(cellConfigurationService.textOf(manifestParentResource),
					cellConfigurationService.graphicOf(manifestParentResource));
			header.getStyleClass().add(Styles.TEXT_BOLD);

			TitledPane pane = new TitledPane();
			pane.setGraphic(header);
			pane.setContent(content);
			return pane;
		}

		// No header, just return the content.
		return content;
	}

	@Nonnull
	private HBox createPermissionRow(@Nonnull PermissionDisplay permission) {
		Label permissionLabel = new Label(permission.permissionName());
		HBox row = new HBox(10, permissionLabel);
		row.setPadding(new Insets(0, 0, 0, 15));

		AndroidPermissionLevel level = permission.level();
		if (!level.rawLevel().equals(level.baseLevel())) {
			Label rawLevel = new Label("(" + level.rawLevel() + ")");
			rawLevel.getStyleClass().add(Styles.TEXT_SUBTLE);
			row.getChildren().add(rawLevel);
		}

		return row;
	}

	@Nonnull
	private static List<String> orderedLevelNames(@Nonnull Map<String, Collection<PermissionDisplay>> levelGroups) {
		List<String> names = new ArrayList<>(levelGroups.keySet());
		names.sort((left, right) -> {
			int leftOrder = LEVEL_ORDER.indexOf(left);
			int rightOrder = LEVEL_ORDER.indexOf(right);
			if (leftOrder >= 0 && rightOrder >= 0)
				return Integer.compare(leftOrder, rightOrder);
			if (leftOrder >= 0)
				return -1;
			if (rightOrder >= 0)
				return 1;
			return left.compareTo(right);
		});
		return names;
	}

	private record PermissionDisplay(@Nonnull String permissionName,
	                                 @Nonnull AndroidPermissionLevel level) implements Comparable<PermissionDisplay> {
		@Override
		public int compareTo(PermissionDisplay o) {
			return permissionName.compareTo(o.permissionName);
		}
	}
}
