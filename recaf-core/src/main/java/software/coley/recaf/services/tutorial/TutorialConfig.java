package software.coley.recaf.services.tutorial;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.util.DevDetection;

/**
 * Config for in-application tutorials.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TutorialConfig extends BasicConfigContainer {
	private final ObservableBoolean acknowledgedSaveWithErrors = new ObservableBoolean(DevDetection.isDevEnv());
	private final ObservableBoolean finishedTutorial = new ObservableBoolean(DevDetection.isDevEnv());

	@Inject
	public TutorialConfig() {
		super(ConfigGroups.SERVICE_UI, "tutorial" + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("acknowledged-save-with-errors", boolean.class, acknowledgedSaveWithErrors, true));
		addValue(new BasicConfigValue<>("finished-tutorial", boolean.class, finishedTutorial, true));
	}

	/**
	 * @return Flag indicating if the user has acknowledged they cannot save with errors.
	 */
	@Nonnull
	public ObservableBoolean getAcknowledgedSaveWithErrors() {
		return acknowledgedSaveWithErrors;
	}

	/**
	 * @return Flag indicating if the user has finished the interactive tutorial.
	 */
	@Nonnull
	public ObservableBoolean getFinishedTutorial() {
		return finishedTutorial;
	}
}
