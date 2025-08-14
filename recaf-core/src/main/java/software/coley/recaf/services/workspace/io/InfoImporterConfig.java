package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableObject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link InfoImporter}
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class InfoImporterConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableObject<ClassPatchMode> classPatchMode = new ObservableObject<>(ClassPatchMode.CHECK_BASIC_THEN_FILTER);

	@Inject
	public InfoImporterConfig() {
		super(ConfigGroups.SERVICE_IO, InfoImporter.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("class-patch-mode", ClassPatchMode.class, classPatchMode));
	}

	/**
	 * You better know what you're doing if you choose a lower tier value on this.
	 * The default is {@link ClassPatchMode#CHECK_BASIC_THEN_FILTER} because otherwise
	 * there will be errors when invalid classes are found. However, in some cases
	 * it may be beneficial to use {@link ClassPatchMode#ALWAYS_FILTER}.
	 * Only use {@link ClassPatchMode#SKIP_FILTER} when looking at unobfuscated classes.
	 *
	 * @return Class patch validation mode.
	 */
	@Nonnull
	public ClassPatchMode getClassPatchMode() {
		return classPatchMode.getValue();
	}

	/**
	 * Level of class pre-processing to take when importing {@link ClassInfo} types.
	 */
	public enum ClassPatchMode {
		/**
		 * Always pre-process classes.
		 */
		ALWAYS_FILTER,
		/**
		 * Check thoroughly for problems in class files before pre-processing them.
		 */
		CHECK_ADVANCED_THEN_FILTER,
		/**
		 * Check quickly for problems in class files before pre-processing them.
		 */
		CHECK_BASIC_THEN_FILTER,
		/**
		 * Do not pre-process classes.
		 */
		SKIP_FILTER
	}
}