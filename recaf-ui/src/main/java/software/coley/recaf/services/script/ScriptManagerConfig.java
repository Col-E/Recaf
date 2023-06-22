package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;
import software.coley.recaf.services.file.RecafDirectoriesConfig;

import java.nio.file.Path;

/**
 * Config for {@link ScriptManager}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ScriptManagerConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableBoolean fileWatching = new ObservableBoolean(true);
	private final RecafDirectoriesConfig directories;

	@Inject
	public ScriptManagerConfig(RecafDirectoriesConfig directories) {
		super(ConfigGroups.SERVICE_PLUGIN, ScriptManager.SERVICE_ID + CONFIG_SUFFIX);
		this.directories = directories;
		addValue(new BasicConfigValue<>("file-watching", Boolean.class, fileWatching));
	}

	/**
	 * @return Directory containing local scripts.
	 */
	@Nonnull
	public Path getScriptsDirectory() {
		return directories.getScriptsDirectory();
	}

	/**
	 * @return {@code true} to enable file watching in {@link ScriptManager} to automatically update available scripts.
	 */
	@Nonnull
	public ObservableBoolean getFileWatching() {
		return fileWatching;
	}
}
