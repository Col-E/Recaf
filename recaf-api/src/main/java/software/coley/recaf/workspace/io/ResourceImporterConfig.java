package software.coley.recaf.workspace.io;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.llzip.ZipIO;
import software.coley.llzip.format.model.ZipArchive;
import software.coley.observables.ObservableObject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;
import software.coley.recaf.util.UncheckedFunction;

/**
 * Config for {@link ResourceImporter}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ResourceImporterConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableObject<ZipStrategy> zipStrategy = new ObservableObject<>(ZipStrategy.JVM);

	@Inject
	public ResourceImporterConfig() {
		super(ConfigGroups.SERVICE_IO, ResourceImporter.SERVICE_ID + CONFIG_SUFFIX);

		addValue(new BasicConfigValue<>("zip-strategy", ZipStrategy.class, zipStrategy));
	}

	/**
	 * @return ZIP strategy.
	 */
	public ObservableObject<ZipStrategy> getZipStrategy() {
		return zipStrategy;
	}

	/**
	 * Mirrors strategies available in {@link ZipIO}.
	 */
	public enum ZipStrategy {
		JVM,
		NAIVE;

		public UncheckedFunction<byte[], ZipArchive> mapping() {
			if (this == JVM) return ZipIO::readJvm;
			else return ZipIO::readStandard;
		}
	}
}
