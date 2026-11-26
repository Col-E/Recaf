package software.coley.recaf.services.config.factories;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import software.coley.observables.Observable;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.services.config.KeyedConfigComponentFactory;
import software.coley.recaf.services.workspace.io.ApkSigningConfig;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.popup.ApkSigningCredentialsPopup;
import software.coley.recaf.util.Lang;

/**
 * Factory for the {@link ApkSigningConfig#KEY_CREDENTIALS} value.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ApkSigningCredentialsComponentFactory extends KeyedConfigComponentFactory<String> {
	private final ApkSigningConfig signingConfig;

	@Inject
	public ApkSigningCredentialsComponentFactory(@Nonnull ApkSigningConfig signingConfig) {
		super(false, id(signingConfig));
		this.signingConfig = signingConfig;
	}

	@Nonnull
	@Override
	public Node create(@Nonnull ConfigContainer container, @Nonnull ConfigValue<String> value) {
		Observable<String> observable = value.getObservable();

		Label state = new Label();
		Runnable refreshState = () -> {
			String envelope = observable.getValue();
			state.setText(Lang.get(envelope == null || envelope.isBlank()
					? "service.io.apk-signing-config.credentials.not-configured"
					: "service.io.apk-signing-config.credentials.configured"));
		};
		refreshState.run();
		observable.addChangeListener((ob, old, cur) -> refreshState.run());

		ActionButton configure = new ActionButton(Lang.get("dialog.configure"), () -> new ApkSigningCredentialsPopup(signingConfig).show());
		Runnable refreshConfigure = () -> configure.setDisable(!signingConfig.hasKeystorePath());
		refreshConfigure.run();
		signingConfig.getKeystorePath().addChangeListener((ob, old, cur) -> refreshConfigure.run());
		HBox layout = new HBox(10, state, configure);
		layout.setAlignment(Pos.CENTER_LEFT);
		return layout;
	}

	@Nonnull
	private static String id(@Nonnull ConfigContainer container) {
		return container.getGroupAndId() + ConfigGroups.PACKAGE_SPLIT + ApkSigningConfig.KEY_CREDENTIALS;
	}
}
