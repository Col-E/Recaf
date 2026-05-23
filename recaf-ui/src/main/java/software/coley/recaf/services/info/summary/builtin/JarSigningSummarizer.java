package software.coley.recaf.services.info.summary.builtin;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.collections.box.Box;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.services.analysis.metadata.FileMetadataAnalysisService;
import software.coley.recaf.services.analysis.metadata.JarCertificateResult;
import software.coley.recaf.services.analysis.metadata.JarSigningReport;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Summarizer that shows jar signature information, with the option to remove it in a single click.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class JarSigningSummarizer implements ResourceSummarizer {
	private static final String MANIFEST_PATCH_PATTERN = "^Name: [\\S\\s]+?[\\w-]+-Digest: .+\\s+?";
	private final FileMetadataAnalysisService fileMetadataAnalysisService;

	@Inject
	public JarSigningSummarizer(@Nonnull FileMetadataAnalysisService fileMetadataAnalysisService) {
		this.fileMetadataAnalysisService = fileMetadataAnalysisService;
	}

	@Override
	public boolean summarize(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull SummaryConsumer consumer) {
		JarSigningReport report = fileMetadataAnalysisService.analyzeJarSigning(workspace, resource);
		if (report == null)
			return false;

		FileBundle bundle = resource.getFileBundle();
		FileInfo manifest = report.manifestPath().getValue();

		// Layout output
		VBox box = new VBox();
		box.setSpacing(15);
		box.setFillWidth(true);
		Insets rightPadding = new Insets(0, 30, 0, 0);
		Label title = new BoundLabel(Lang.getBinding("service.analysis.signature-info"));
		title.getStyleClass().addAll(Styles.TITLE_4);
		for (JarCertificateResult certificateResult : report.certificateResults()) {
			if (certificateResult.parseError() != null) {
				box.getChildren().add(new Label(certificateResult.parseError()));
				continue;
			}
			CodeArea area = new CodeArea();
			area.getStyleClass().addAll("background-dark", "border-muted");
			for (var certificate : certificateResult.certificates()) {
				area.appendText(certificate.toString() + "\n");
				area.appendText("=".repeat(73) + "\n");
			}
			area.showParagraphAtTop(0);
			area.setPrefHeight(300);
			area.setEditable(false);
			BorderPane pane = new BorderPane(new VirtualizedScrollPane<>(area));
			pane.setPadding(rightPadding);
			box.getChildren().add(pane);
		}
		Box<Runnable> boxedFixRunnable = new Box<>();
		Button button = new ActionButton(new FontIconView(CarbonIcons.TRASH_CAN), Lang.getBinding("menu.edit.remove"), () -> boxedFixRunnable.get().run());
		HBox titleWrapper = new HBox(title, new Spacer(), button);
		titleWrapper.setPadding(rightPadding);
		box.getChildren().addFirst(titleWrapper);
		consumer.appendSummary(box);

		// Create task to remove signing information
		Runnable fix = () -> {
			// Patch out the entries from the 'MANIFEST.MF'
			String patchedManifest = RegexUtil.getMatcher(MANIFEST_PATCH_PATTERN, manifest.asTextFile().getText()).replaceAll("");
			bundle.put(manifest.asTextFile().toTextBuilder().withText(patchedManifest).build());

			// Remove '.SF' and '.RSA' files
			for (var signaturePath : report.signatureFilePaths())
				bundle.remove(signaturePath.getValue().getName());

			// Indicate we're done
			FxThreadUtil.run(() -> {
				box.getChildren().clear();
				box.getChildren().add(title);
				title.setContentDisplay(ContentDisplay.RIGHT);
				title.setGraphic(new BoundLabel(Lang.getBinding("misc.removed").map(s -> "(" + s + ")")));
			});
		};
		boxedFixRunnable.set(fix);

		// Indicate we have emitted our summary
		return true;
	}
}
