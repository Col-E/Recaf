package software.coley.recaf.services.info.summary.builtin;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.collections.box.Box;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.TextFileInfo;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Summarizer that shows jar signature information, with the option to remove it in a single click.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class JarSigningSummarizer implements ResourceSummarizer {
	private static final String MANIFEST_PATCH_PATTERN = "Name: [\\S\\s]+?[\\w-]+-Digest: .+\\s+?";
	private static final CertificateFactory CERTIFICATE_FACTORY;

	static {
		CertificateFactory factory;
		try {
			factory = CertificateFactory.getInstance("X.509");
		} catch (Throwable t) {
			factory = null;
		}
		CERTIFICATE_FACTORY = factory;
	}

	@Override
	public boolean summarize(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull SummaryConsumer consumer) {
		FileBundle bundle = resource.getFileBundle();
		FileInfo manifest = bundle.get("META-INF/MANIFEST.MF");

		// Must have a manifest file
		if (manifest == null || !manifest.isTextFile())
			return false;

		// Must list at least one file signed
		TextFileInfo manifestText = manifest.asTextFile();
		if (!manifestText.getText().contains("-Digest: "))
			return false;

		// Collect signature files to remove
		List<FileInfo> sfFiles = new ArrayList<>();
		List<FileInfo> rsaFiles = new ArrayList<>();
		for (FileInfo file : bundle) {
			String name = file.getName();
			if (name.matches("META-INF/[\\w-]+\\.(?:SF|RSA)")) {
				if (name.endsWith(".SF")) {
					sfFiles.add(file);
				} else {
					rsaFiles.add(file);
				}
			}
		}

		// Layout output
		VBox box = new VBox();
		box.setSpacing(15);
		box.setFillWidth(true);
		Insets rightPadding = new Insets(0, 30, 0, 0);
		Label title = new BoundLabel(Lang.getBinding("service.analysis.signature-info"));
		title.getStyleClass().addAll(Styles.TITLE_4);
		for (FileInfo rsaFile : rsaFiles) {
			try {
				CodeArea area = new CodeArea();
				area.getStylesheets().add("/style/code-editor.css");
				area.getStyleClass().addAll("background-dark", "border-muted");
				Collection<? extends Certificate> certificates = CERTIFICATE_FACTORY.generateCertificates(new ByteArrayInputStream(rsaFile.getRawContent()));
				for (Certificate certificate : certificates) {
					area.appendText(certificate.toString() + "\n");
					area.appendText("=".repeat(73) + "\n");
				}
				area.showParagraphAtTop(0);
				area.setPrefHeight(300);
				area.setEditable(false);
				BorderPane pane = new BorderPane(new VirtualizedScrollPane<>(area));
				pane.setPadding(rightPadding);
				box.getChildren().add(pane);
			} catch (CertificateException e) {
				box.getChildren().add(new Label("Error parsing certificate: " + rsaFile.getName()));
			}
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
			String patchedManifest = RegexUtil.getMatcher(MANIFEST_PATCH_PATTERN, manifestText.getText()).replaceAll("");
			bundle.put(manifestText.toTextBuilder().withText(patchedManifest).build());

			// Remove '.SF' and '.RSA' files
			for (FileInfo file : sfFiles)
				bundle.remove(file.getName());
			for (FileInfo file : rsaFiles)
				bundle.remove(file.getName());

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
