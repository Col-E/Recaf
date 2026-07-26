package software.coley.recaf.ui.pane.sample;

import software.coley.recaf.plugin.Plugin;
import software.coley.recaf.plugin.PluginInformation;

/**
 * Sample plugin used by {@code PluginManagerPaneTest} to build real plugin jars.
 *
 * @author Canrad
 */
@PluginInformation(id = "sample-alpha", name = "Sample Alpha", version = "1.0.0",
		author = "tester", description = "First sample plugin.")
public class SampleAlphaPlugin implements Plugin {
	@Override
	public void onEnable() {}

	@Override
	public void onDisable() {}
}
