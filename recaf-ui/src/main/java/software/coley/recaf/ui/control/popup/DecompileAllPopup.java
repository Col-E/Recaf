package software.coley.recaf.ui.control.popup;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.observables.ObservableObject;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.decompile.JvmDecompiler;
import software.coley.recaf.ui.config.RecentFilesConfig;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.ObservableComboBox;
import software.coley.recaf.ui.pane.editing.jvm.DecompilerPaneConfig;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.ui.window.RecafStage;
import software.coley.recaf.util.*;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Popup for initiating decompilation of all classes, saved to a specified location.
 *
 * @author Matt Coley
 */
@Dependent
public class DecompileAllPopup extends RecafStage {
	private static final Logger logger = Logging.get(DecompileAllPopup.class);
	private final ObjectProperty<Path> pathProperty = new SimpleObjectProperty<>();
	private final ObservableObject<JvmDecompiler> decompilerProperty;
	private final BooleanProperty inProgressProperty = new SimpleBooleanProperty();
	private JvmClassBundle targetBundle;

	@Inject
	public DecompileAllPopup(@Nonnull DecompilerManager decompilerManager,
	                         @Nonnull RecentFilesConfig recentFilesConfig,
	                         @Nonnull DecompilerPaneConfig decompilerPaneConfig,
	                         @Nonnull Workspace workspace) {
		String defaultName = buildName(workspace);

		targetBundle = workspace.getPrimaryResource().getJvmClassBundle();
		decompilerProperty = new ObservableObject<>(decompilerManager.getTargetJvmDecompiler());
		pathProperty.setValue(Paths.get(recentFilesConfig.getLastWorkspaceExportDirectory().getValue()).resolve(defaultName));

		Label decompilerLabel = new BoundLabel(Lang.getBinding("java.decompiler"));
		Label pathLabel = new BoundLabel(Lang.getBinding("menu.file.decompileall.path"));
		ObservableComboBox<JvmDecompiler> decompilerCombo = new ObservableComboBox<>(decompilerProperty, decompilerManager.getJvmDecompilers());
		ProgressBar progress = new ProgressBar(0);
		Button pathButton = new ActionButton(CarbonIcons.EDIT, pathProperty.map(Path::toString), () -> {
			FileChooser chooser = new FileChooserBuilder()
					.setInitialFileName(defaultName)
					.setInitialDirectory(recentFilesConfig.getLastWorkspaceExportDirectory())
					.setFileExtensionFilter("Archives", "*.zip", "*.jar")
					.setTitle(Lang.get("dialog.file.open"))
					.build();
			File file = chooser.showSaveDialog(getScene().getWindow());
			if (file != null) {
				String parent = file.getParent();
				if (parent != null) recentFilesConfig.getLastWorkspaceOpenDirectory().setValue(parent);
				pathProperty.set(file.toPath());
			}
		});
		Button decompileButton = new ActionButton(CarbonIcons.SAVE_SERIES, Lang.getBinding("menu.file.decompileall"), () -> {
			try {
				inProgressProperty.setValue(true);
				progress.setProgress(0);

				// Determine which classes to decompile
				List<JvmClassInfo> targetClasses = targetBundle.stream().filter(cls -> {
					// Skip inner classes
					if (cls.isInnerClass())
						return false;

					// Skip special case classes like 'module-info' and 'package-info'
					String name = cls.getName();
					return cls.getSuperName() != null || (!name.equals("module-info") && !name.endsWith("package-info"));
				}).toList();

				// Determine delta of each decompilation
				int targetCount = targetClasses.size();
				AtomicInteger actionedClasses = new AtomicInteger(targetCount);

				// Decompile all classes
				JvmDecompiler decompiler = decompilerProperty.getValue();
				ZipCreationUtils.ZipBuilder builder = ZipCreationUtils.builder();
				targetClasses.forEach(cls -> {
					String name = cls.getName();
					decompilerManager.decompile(decompiler, workspace, cls)
							.orTimeout(decompilerPaneConfig.getTimeoutSeconds().getValue(), TimeUnit.SECONDS)
							.whenComplete((result, error) -> {
								int remaining = actionedClasses.decrementAndGet();
								if (result != null) {
									// Handle errors
									if (result.getException() != null) {
										logger.error("Failed to decompile '{}'", name, result.getException());
										return;
									}

									// Write decompilation output
									String text = result.getText();
									if (text != null)
										builder.add(name + ".java", text.getBytes(StandardCharsets.UTF_8));
								} else {
									logger.error("Failed to decompile '{}'", name, error);
								}

								// If done, write the zip file
								if (remaining <= 0) {
									inProgressProperty.setValue(false);
									Path path = pathProperty.get();
									try {
										Files.write(path, builder.bytes());
									} catch (IOException ex) {
										logger.error("Failed to write archive of decompiled classes to '{}'", path, ex);
									}
								}
							}).thenRunAsync(() -> progress.setProgress(1 - (actionedClasses.doubleValue() / targetCount)), FxThreadUtil.executor());
				});
			} catch (Throwable t) {
				logger.error("Failed to schedule all classes for decompilation", t);
				inProgressProperty.setValue(false);
			}
		});
		decompileButton.disableProperty().bind(pathProperty.isNull().or(inProgressProperty));

		// Layout
		GridPane layout = new GridPane(8, 8);
		GridPane.setFillWidth(progress, true);
		GridPane.setFillWidth(decompilerCombo, true);
		GridPane.setFillWidth(decompileButton, true);
		GridPane.setFillWidth(pathButton, true);
		pathButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		decompileButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		decompilerCombo.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		progress.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		layout.add(decompilerLabel, 0, 0);
		layout.add(decompilerCombo, 1, 0);
		layout.add(pathLabel, 0, 1);
		layout.add(pathButton, 1, 1);
		layout.add(decompileButton, 1, 2);
		layout.add(progress, 0, 3, 2, 1);
		layout.setPadding(new Insets(10));
		layout.setAlignment(Pos.TOP_CENTER);

		setMinWidth(450);
		setMinHeight(200);
		setTitle(Lang.get("menu.file.decompileall"));
		setScene(new RecafScene(layout, 400, 150));
	}

	@Nonnull
	private static String buildName(@Nonnull Workspace workspace) {
		String prefix = "";
		if (workspace.getPrimaryResource() instanceof WorkspaceFileResource fileResource)
			prefix = StringUtil.removeExtension(StringUtil.shortenPath(fileResource.getFileInfo().getName())) + "-";
		return prefix + "decompiled.zip";
	}

	/**
	 * @param targetBundle
	 * 		Bundle to target for decompilation.
	 */
	public void setTargetBundle(@Nonnull JvmClassBundle targetBundle) {
		this.targetBundle = targetBundle;
	}
}
