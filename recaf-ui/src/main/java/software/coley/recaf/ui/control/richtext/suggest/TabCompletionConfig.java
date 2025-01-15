package software.coley.recaf.ui.control.richtext.suggest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableObject;
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
	private final ObservableObject<PopupPosition> popupPosition = new ObservableObject<>(PopupPosition.ABOVE_RIGHT);

	@Inject
	public TabCompletionConfig() {
		super(ConfigGroups.SERVICE_UI, "tab-completion" + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("enabled-in-assembler", boolean.class, enabledInAssembler));
		addValue(new BasicConfigValue<>("popup-position", PopupPosition.class, popupPosition));
	}

	/**
	 * @return {@code true} when tab completion in the {@link AssemblerPane} should be registered.
	 */
	public boolean isEnabledInAssembler() {
		return enabledInAssembler.getValue();
	}

	/**
	 * @return Current popup position.
	 */
	public PopupPosition getPopupPosition() {
		return popupPosition.getValue();
	}

	public enum PopupPosition {
		/**
		 * Popup appears above and to right of the cursor
		 */
		ABOVE_RIGHT,
		/**
		 * Popup appears below and to right of the cursor
		 */
		BELOW_RIGHT,
		/**
		 * Popup appears above and to left of the cursor
		 */
		ABOVE_LEFT,
		/**
		 * Popup appears below and to left of the cursor
		 */
		BELOW_LEFT;

		public boolean isAbove() {
			return this == ABOVE_RIGHT || this == ABOVE_LEFT;
		}

		public boolean isRight() {
			return this == ABOVE_RIGHT || this == BELOW_RIGHT;
		}
	}
}
