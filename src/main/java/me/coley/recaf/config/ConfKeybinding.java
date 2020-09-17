package me.coley.recaf.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.plugin.PluginKeybinds;
import me.coley.recaf.util.Log;

/**
 * Keybind configuration.
 *
 * @author Matt
 */
public class ConfKeybinding extends Config {
	/**
	 * Save current application to file.
	 */
	@Conf("binding.saveapp")
	public Binding saveApp = Binding.from(KeyCode.CONTROL, KeyCode.E);
	/**
	 * Save current work.
	 */
	@Conf("binding.save")
	public Binding save = Binding.from(KeyCode.CONTROL, KeyCode.S);
	/**
	 * Undo last change.
	 */
	@Conf("binding.undo")
	public Binding undo = Binding.from(KeyCode.CONTROL, KeyCode.U);
	/**
	 * Open find search.
	 */
	@Conf("binding.find")
	public Binding find = Binding.from(KeyCode.CONTROL, KeyCode.F);
	/**
	 * Close top-most window <i>(Except the main window)</i>
	 */
	@Conf("binding.close.window")
	public Binding closeWindow = Binding.from(KeyCode.CONTROL, KeyCode.ESCAPE);
	/**
	 * Close current file/class tab.
	 */
	@Conf("binding.close.tab")
	public Binding closeTab = Binding.from(KeyCode.CONTROL, KeyCode.W);
	/**
	 * Goto the selected item's definition.
	 */
	@Conf("binding.gotodef")
	public Binding gotoDef = Binding.from(KeyCode.F3);
	/**
	 * Goto the selected item's definition.
	 */
	@Conf("binding.rename")
	public Binding rename = Binding.from(KeyCode.CONTROL, KeyCode.R);

	ConfKeybinding() {
		super("keybinding");
	}

	@Override
	public boolean supported(Class<?> type) {
		return type.equals(Binding.class);
	}

	/**
	 * Register window-level keybinds.
	 *
	 * @param controller
	 * 		Controller to call actions on.
	 * @param stage
	 * 		Window to register keys on.
	 * @param scene
	 * 		Scene in the window.
	 */
	public void registerWindowKeys(GuiController controller, Stage stage, Scene scene) {
		scene.setOnKeyReleased(e -> handleWindowKeyEvents(e, controller, stage, false));
	}

	/**
	 * Register window-level keybinds.
	 *
	 * @param controller
	 * 		Controller to call actions on.
	 * @param stage
	 * 		Window to register keys on.
	 * @param scene
	 * 		Scene in the window.
	 */
	public void registerMainWindowKeys(GuiController controller, Stage stage, Scene scene) {
		scene.setOnKeyReleased(e -> handleWindowKeyEvents(e, controller, stage, true));
	}

	private void handleWindowKeyEvents(KeyEvent e, GuiController controller, Stage stage, boolean main) {
		if(!main && closeWindow.match(e))
			stage.close();
		if(saveApp.match(e))
			controller.windows().getMainWindow().saveApplication();
		else {
			// Custom bind support
			PluginKeybinds.getInstance().getGlobalBinds().forEach((bind, action) -> {
				try {
					if (bind.match(e))
						action.run();
				} catch(Throwable t) {
					Log.error(t, "Failed executing global keybind action");
				}
			});
		}
	}

	/**
	 * Wrapper to act as bind utility.
	 *
	 * @author Matt
	 */
	public static class Binding extends ArrayList<String> {
		/**
		 * @param string
		 * 		Series of keys for a keybind, split by '+'.
		 * @param mask
		 * 		Optional mask to include.
		 *
		 * @return Binding from keys + mask.
		 */
		public static Binding from(String string, KeyCode mask) {
			String[] codes = string.split("\\+");
			Stream<String> stream = Arrays.stream(codes);
			if (mask != null)
				stream = Stream.concat(Stream.of(mask.getName()), stream);
			return stream.map(String::toLowerCase).collect(Collectors.toCollection(Binding::new));
		}

		/**
		 * @param event
		 * 		Key event.
		 *
		 * @return Binding from keys of the event.
		 */
		public static Binding from(KeyEvent event) {
			return from(namesOf(event));
		}

		/**
		 * @param codes
		 * 		Series of JFX KeyCodes for a keybind. Unlike {@link #from(String, KeyCode)}
		 * 		it is implied that the mask is given in this series, if one is intended.
		 *
		 * @return Binding from keys.
		 */
		public static Binding from(KeyCode... codes) {
			return Arrays.stream(codes).map(KeyCode::getName)
					.map(String::toLowerCase)
					.collect(Collectors.toCollection(Binding::new));
		}

		/**
		 * @param codes
		 * 		Series of keys for a keybind.
		 *
		 * @return Binding from keys.
		 */
		public static Binding from(Collection<String> codes) {
			return codes.stream()
						.map(String::toLowerCase)
						.sorted((a, b) -> (a.length() > b.length()) ? -1 : a.compareTo(b))
						.collect(Collectors.toCollection(Binding::new));
		}

		@Override
		public String toString() {
			return String.join("+", this);
		}

		/**
		 * @param event
		 * 		JFX key event.
		 *
		 * @return {@code true} if the event matches the current bind.
		 */
		public boolean match(KeyEvent event) {
			if (size() == 1)
				// Simple 1-key check
				return event.getCode().getName().equalsIgnoreCase(get(0));
			else if (size() > 1) {
				// Checking binds with masks
				Set<String> bindSet = new HashSet<>(this);
				Set<String> eventSet = namesOf(event);
				return bindSet.equals(eventSet);
			} else
				throw new IllegalStateException("Keybind must have have at least 1 key!");
		}

		private static Set<String> namesOf(KeyEvent event) {
			Set<String> eventSet = new HashSet<>();
			eventSet.add(event.getCode().getName().toLowerCase());
			if (event.isControlDown())
				eventSet.add("ctrl");
			else if (event.isAltDown())
				eventSet.add("alt");
			else if (event.isShiftDown())
				eventSet.add("shift");
            else if (event.isMetaDown())
                eventSet.add("meta");
			return eventSet;
		}
	}
}
