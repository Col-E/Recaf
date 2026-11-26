package software.coley.recaf.services.config.factories;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import software.coley.observables.Observable;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.services.config.KeyedConfigComponentFactory;
import software.coley.recaf.services.workspace.io.ApkSigningConfig;
import software.coley.recaf.util.Lang;

/**
 * Factory for the APK signing toggle, disabled until a keystore path is configured.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ApkSigningEnabledComponentFactory extends KeyedConfigComponentFactory<Boolean> {
	private final ApkSigningConfig signingConfig;

	@Inject
	public ApkSigningEnabledComponentFactory(@Nonnull ApkSigningConfig signingConfig) {
		super(true, id(signingConfig));
		this.signingConfig = signingConfig;
	}

	@Nonnull
	@Override
	public Node create(@Nonnull ConfigContainer container, @Nonnull ConfigValue<Boolean> value) {
		Observable<Boolean> enabled = value.getObservable();
		CheckBox check = new CheckBox();
		check.textProperty().bind(Lang.getBinding(container.getScopedId(value)));

		Runnable refresh = () -> {
			boolean canSign = signingConfig.hasKeystorePath();
			check.setDisable(!canSign);
			if (!canSign && enabled.getValue())
				enabled.setValue(false);
			if (check.isSelected() != enabled.getValue())
				check.setSelected(enabled.getValue());
		};
		refresh.run();

		signingConfig.getKeystorePath().addChangeListener((ob, old, cur) -> refresh.run());
		enabled.addChangeListener((ob, old, cur) -> refresh.run());
		check.selectedProperty().addListener((ob, old, cur) -> {
			if (cur && !signingConfig.hasKeystorePath()) {
				check.setSelected(false);
				return;
			}
			if (enabled.getValue() != cur)
				enabled.setValue(cur);
		});
		return check;
	}

	@Nonnull
	private static String id(@Nonnull ConfigContainer container) {
		return container.getGroupAndId() + ConfigGroups.PACKAGE_SPLIT + ApkSigningConfig.KEY_ENABLED;
	}
}
