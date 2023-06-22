package software.coley.recaf.ui.pane.editing.jvm;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.source.AstMappingVisitor;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.pane.editing.android.AndroidDecompilerPane;

import java.io.File;

/**
 * Config for {@link JvmDecompilerPane} and {@link AndroidDecompilerPane}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class DecompilerPaneConfig extends BasicConfigContainer {
	private final ObservableInteger timeoutSeconds = new ObservableInteger(60);
	private final ObservableBoolean useMappingAcceleration = new ObservableBoolean(true);
	private final ObservableBoolean acknowledgedSaveWithErrors = new ObservableBoolean(isDevEnv());

	@Inject
	public DecompilerPaneConfig() {
		super(ConfigGroups.SERVICE_UI, "decompile-pane" + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("timeout-seconds", Integer.class, timeoutSeconds));
		addValue(new BasicConfigValue<>("mapping-acceleration", Boolean.class, useMappingAcceleration));
		addValue(new BasicConfigValue<>("acknowledged-save-with-errors", Boolean.class, acknowledgedSaveWithErrors, true));
	}

	/**
	 * @return Decompilation timeout in seconds.
	 */
	@Nonnull
	public ObservableInteger getTimeoutSeconds() {
		return timeoutSeconds;
	}

	/**
	 * @return Flag indicating if the user has acknowledged they cannot save with errors.
	 */
	@Nonnull
	public ObservableBoolean getAcknowledgedSaveWithErrors() {
		return acknowledgedSaveWithErrors;
	}

	/**
	 * @return Flag indicating if {@link AstMappingVisitor} should be used to update {@link Editor#getText()}
	 * instead of decompiling the new remapped class. Generally faster, but can be inaccurate in some edge cases.
	 */
	@Nonnull
	public ObservableBoolean getUseMappingAcceleration() {
		return useMappingAcceleration;
	}

	private static boolean isDevEnv() {
		// Should only be true when building Recaf from source/build-system.
		return System.getProperty("java.class.path").contains("recaf-ui" + File.separator + "build");
	}
}
