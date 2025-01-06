package software.coley.recaf.services.workspace.io;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link InfoImporter}
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class InfoImporterConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableBoolean skipClassAsmValidation = new ObservableBoolean(false);
	private final ObservableBoolean skipCodeParing = new ObservableBoolean(true);

	@Inject
	public InfoImporterConfig() {
		super(ConfigGroups.SERVICE_IO, InfoImporter.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("skip-class-asm-validation", boolean.class, skipClassAsmValidation));
		addValue(new BasicConfigValue<>("skip-class-code", boolean.class, skipCodeParing));
	}

	/**
	 * You better know what you're doing if you disable this. The default is {@code false} because
	 * otherwise there will be errors when invalid classes are found.
	 *
	 * @return {@code true} to skip validation of classes when importing classes.
	 */
	public boolean doSkipAsmValidation() {
		return skipClassAsmValidation.getValue();
	}

	/**
	 * Skips parsing method code when importing.
	 * This results in not populating {@link LocalVariable} values in {@link MethodMember}s.
	 * Enabling this saves a decent bit of time, around ~25% of the input processing time on my machine.
	 *
	 * @return {@code true} to skip parsing method code attributes when importing classes.
	 */
	public boolean doSkipCodeParing() {
		return skipCodeParing.getValue();
	}
}