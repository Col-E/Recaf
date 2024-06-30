package software.coley.recaf.services.config.factories;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.Node;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.services.config.KeyedConfigComponentFactory;
import software.coley.recaf.services.decompile.Decompiler;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.decompile.DecompilerManagerConfig;
import software.coley.recaf.ui.control.ObservableComboBox;

import java.util.List;

/**
 * Factory for general {@link DecompilerManagerConfig#getPreferredJvmDecompiler()}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class JvmDecompilerComponentFactory extends KeyedConfigComponentFactory<String> {
	private final List<String> decompilers;

	@Inject
	public JvmDecompilerComponentFactory(@Nonnull DecompilerManager decompilerManager) {
		super(false, id(decompilerManager.getServiceConfig()));
		decompilers = decompilerManager.getJvmDecompilers().stream()
				.map(Decompiler::getName)
				.toList();
	}

	@Nonnull
	@Override
	public Node create(@Nonnull ConfigContainer container, @Nonnull ConfigValue<String> value) {
		return new ObservableComboBox<>(value.getObservable(), decompilers);
	}

	@Nonnull
	private static String id(@Nonnull ConfigContainer container) {
		return container.getGroupAndId() + ConfigGroups.PACKAGE_SPLIT + DecompilerManagerConfig.KEY_PREF_JVM_DECOMPILER;
	}
}
