package software.coley.recaf.services.script;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link ScriptEngine}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ScriptEngineConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public ScriptEngineConfig() {
		super(ConfigGroups.SERVICE_PLUGIN, ScriptEngine.SERVICE_ID + CONFIG_SUFFIX);
	}
}
