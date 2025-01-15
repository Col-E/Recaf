package software.coley.recaf.ui.control.richtext.suggest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.ui.pane.editing.assembler.AssemblerPane;

/**
 * Config for {@link TabCompleter} use cases.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TabCompletionConfig extends BasicConfigContainer {
	private final ObservableBoolean enabledInAssembler = new ObservableBoolean(true);

	@Inject
	public TabCompletionConfig() {
		super(ConfigGroups.SERVICE_UI, "tab-completion" + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("enabled-in-assembler", boolean.class, enabledInAssembler));
	}

	/**
	 * @return {@code true} when tab completion in the {@link AssemblerPane} should be registered.
	 */
	public boolean isEnabledInAssembler() {
		return enabledInAssembler.getValue();
	}
}
