package software.coley.recaf.services.config.factories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.Node;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.services.decompile.Decompiler;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.decompile.DecompilerManagerConfig;
import software.coley.recaf.services.config.KeyedConfigComponentFactory;
import software.coley.recaf.ui.control.ObservableComboBox;

import java.util.List;

/**
 * Factory for general {@link DecompilerManagerConfig#getPreferredAndroidDecompiler()}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AndroidDecompilerComponentFactory extends KeyedConfigComponentFactory<String> {
	private final List<String> decompilers;

	@Inject
	public AndroidDecompilerComponentFactory(DecompilerManager decompilerManager) {
		super(false, DecompilerManagerConfig.KEY_PREF_ANDROID_DECOMPILER);
		decompilers = decompilerManager.getAndroidDecompilers().stream()
				.map(Decompiler::getName)
				.toList();
	}

	@Override
	public Node create(ConfigContainer container, ConfigValue<String> value) {
		return new ObservableComboBox<>(value.getObservable(), decompilers);
	}
}
