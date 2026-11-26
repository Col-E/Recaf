package software.coley.recaf.services.config.factories;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import software.coley.observables.Observable;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.services.config.KeyedConfigComponentFactory;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.services.workspace.io.ApkSigningConfig;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.util.FileChooserBuilder;
import software.coley.recaf.util.Lang;

import java.io.File;
import java.util.Objects;

/**
 * Factory for selecting or entering the APK signing keystore path.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ApkSigningKeystorePathComponentFactory extends KeyedConfigComponentFactory<String> {
	private final WindowManager windowManager;

	@Inject
	public ApkSigningKeystorePathComponentFactory(@Nonnull WindowManager windowManager, @Nonnull ApkSigningConfig config) {
		super(false, id(config));
		this.windowManager = windowManager;
	}

	@Nonnull
	@Override
	public Node create(@Nonnull ConfigContainer container, @Nonnull ConfigValue<String> value) {
		Observable<String> observable = value.getObservable();
		TextField pathField = new TextField();
		pathField.setText(Objects.requireNonNullElse(observable.getValue(), ""));
		pathField.textProperty().addListener((ob, old, cur) -> {
			if (!Objects.equals(observable.getValue(), cur))
				observable.setValue(cur);
		});
		observable.addChangeListener((ob, old, cur) -> {
			String path = Objects.requireNonNullElse(cur, "");
			if (!Objects.equals(pathField.getText(), path))
				pathField.setText(path);
		});

		ActionButton browse = new ActionButton(Lang.getBinding("misc.browse"), () -> {
			String currentPath = observable.getValue();
			FileChooserBuilder builder = new FileChooserBuilder()
					.setTitle(Lang.get("service.io.apk-signing-config.select-keystore"))
					.setFileExtensionFilter(Lang.get("service.io.apk-signing-config.keystore-files"),
							"*.jks", "*.keystore", "*.p12", "*.pfx", "*.pkcs12");
			if (currentPath != null && !currentPath.isBlank()) {
				File current = new File(currentPath);
				File initialDirectory = current.isDirectory() ? current : current.getParentFile();
				if (initialDirectory != null)
					builder.setInitialDirectory(initialDirectory);
			}
			File selected = builder.open(windowManager.getMainWindow());
			if (selected != null)
				observable.setValue(selected.getAbsolutePath());
		});

		HBox layout = new HBox(8, pathField, browse);
		layout.setAlignment(Pos.CENTER_LEFT);
		pathField.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(pathField, Priority.ALWAYS);
		return layout;
	}

	@Nonnull
	private static String id(@Nonnull ConfigContainer container) {
		return container.getGroupAndId() + ConfigGroups.PACKAGE_SPLIT + ApkSigningConfig.KEY_KEYSTORE_PATH;
	}
}
