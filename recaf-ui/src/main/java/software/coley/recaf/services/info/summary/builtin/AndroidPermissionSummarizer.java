package software.coley.recaf.services.info.summary.builtin;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.Label;
import software.coley.recaf.services.analysis.android.AndroidAnalysisService;
import software.coley.recaf.services.analysis.android.AndroidPermissionEntry;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.Batch;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Summarizer that shows requested permissions from an Android application.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AndroidPermissionSummarizer implements ResourceSummarizer {
	private final AndroidAnalysisService androidAnalysisService;

	@Inject
	public AndroidPermissionSummarizer(@Nonnull AndroidAnalysisService androidAnalysisService) {
		this.androidAnalysisService = androidAnalysisService;
	}

	@Override
	public boolean summarize(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull SummaryConsumer consumer) {
		var permissions = androidAnalysisService.findRequestedPermissions(workspace, resource);
		if (permissions.isEmpty())
			return false;

		Batch batch = FxThreadUtil.batch();
		batch.add(() -> {
			Label title = new BoundLabel(Lang.getBinding("service.analysis.permissions"));
			title.getStyleClass().addAll(Styles.TITLE_4);
			consumer.appendSummary(title);
		});
		for (AndroidPermissionEntry permission : permissions)
			batch.add(() -> consumer.appendSummary(new Label(permission.permission())));
		batch.execute();
		return true;
	}
}
