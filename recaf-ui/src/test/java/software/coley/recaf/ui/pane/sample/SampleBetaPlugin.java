package software.coley.recaf.ui.pane.sample;

import software.coley.recaf.plugin.Plugin;
import software.coley.recaf.plugin.PluginInformation;

/**
 * Sample plugin used by {@code PluginManagerPaneTest} to build real plugin jars.
 *
 * @author Canrad
 */
@PluginInformation(id = "sample-beta", name = "Sample Beta", version = "2.1.0",
		author = "tester", description = "Second sample plugin.")
public class SampleBetaPlugin implements Plugin {
	@Override
	public void onEnable() {}

	@Override
	public void onDisable() {}
}
