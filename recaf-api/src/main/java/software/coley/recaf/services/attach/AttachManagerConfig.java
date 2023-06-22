package software.coley.recaf.services.attach;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;
import software.coley.recaf.services.file.RecafDirectoriesConfig;

import javax.management.MBeanServerConnection;
import java.nio.file.Path;

/**
 * Config for {@link AttachManager}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AttachManagerConfig extends BasicConfigContainer implements ServiceConfig {
	private final RecafDirectoriesConfig directories;
	private final ObservableBoolean passiveScanning = new ObservableBoolean(false);
	private final ObservableBoolean attachJmxAgent = new ObservableBoolean(true);

	@Inject
	public AttachManagerConfig(RecafDirectoriesConfig directories) {
		super(ConfigGroups.SERVICE_DEBUG, AttachManager.SERVICE_ID + CONFIG_SUFFIX);
		this.directories = directories;
		// Add values
		addValue(new BasicConfigValue<>("passive-scanning", Boolean.class, passiveScanning, true));
		addValue(new BasicConfigValue<>("attach-jmx-bean-agent", Boolean.class, attachJmxAgent));
	}

	/**
	 * @return Mirror of {@link RecafDirectoriesConfig#getAgentDirectory()}.
	 */
	public Path getAgentDirectory() {
		return directories.getAgentDirectory();
	}

	/**
	 * @return {@code true} to enable passive scanning in the {@link AttachManager}.
	 */
	public ObservableBoolean getPassiveScanning() {
		return passiveScanning;
	}

	/**
	 * @return {@code true} to enable attaching the JMX agent to discovered servers,
	 * allowing usage of {@link MBeanServerConnection}.
	 */
	public ObservableBoolean getAttachJmxAgent() {
		return attachJmxAgent;
	}
}
