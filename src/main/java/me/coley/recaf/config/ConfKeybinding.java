package me.coley.recaf.config;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.plugin.PluginKeybinds;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.OSUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	public Binding saveApp = BindingCreator.from(
			Binding.from(KeyCode.CONTROL, KeyCode.E),
			BindingCreator.OSBinding.from(OSUtil.MAC, Binding.from(KeyCode.META, KeyCode.E))
	).buildKeyBindingForCurrentOS();
	/**
	 * Save current work.
	 */
	@Conf("binding.save")
	public Binding save = BindingCreator.from(
			Binding.from(KeyCode.CONTROL, KeyCode.S),
			BindingCreator.OSBinding.from(OSUtil.MAC, Binding.from(KeyCode.META, KeyCode.S))
	).buildKeyBindingForCurrentOS();
	/**
	 * Undo last change.
	 */
	@Conf("binding.undo")
	public Binding undo = BindingCreator.from(
			Binding.from(KeyCode.CONTROL, KeyCode.U),
			BindingCreator.OSBinding.from(OSUtil.MAC, Binding.from(KeyCode.META, KeyCode.U))
	).buildKeyBindingForCurrentOS();
	/**
	 * Open find search.
	 */
	@Conf("binding.find")
	public Binding find = BindingCreator.from(
			Binding.from(KeyCode.CONTROL, KeyCode.F),
			BindingCreator.OSBinding.from(OSUtil.MAC, Binding.from(KeyCode.META, KeyCode.F))
	).buildKeyBindingForCurrentOS();
	/**
	 * Close top-most window <i>(Except the main window)</i>
	 */
	@Conf("binding.close.window")
	public Binding closeWindow = BindingCreator.from(
			Binding.from(KeyCode.CONTROL, KeyCode.ESCAPE),
			BindingCreator.OSBinding.from(OSUtil.MAC, Binding.from(KeyCode.META, KeyCode.ESCAPE))
	).buildKeyBindingForCurrentOS();
	/**
	 * Close current file/class tab.
	 */
	@Conf("binding.close.tab")
	public Binding closeTab = BindingCreator.from(
			Binding.from(KeyCode.CONTROL, KeyCode.W),
			BindingCreator.OSBinding.from(OSUtil.MAC, Binding.from(KeyCode.META, KeyCode.W))
	).buildKeyBindingForCurrentOS();
	/**
	 * Goto the selected item's definition.
	 */
	@Conf("binding.gotodef")
	public Binding gotoDef = Binding.from(KeyCode.F3);
	/**
	 * Goto the selected item's definition.
	 */
	@Conf("binding.rename")
	public Binding rename = BindingCreator.from(
			Binding.from(KeyCode.CONTROL, KeyCode.R),
			BindingCreator.OSBinding.from(OSUtil.MAC, Binding.from(KeyCode.META, KeyCode.R))
	).buildKeyBindingForCurrentOS();
	/**
	 * Swap to the next view mode for the class viewport
	 */
	@Conf("binding.swapview")
	public Binding swapview = BindingCreator.from(
			Binding.from(KeyCode.CONTROL, KeyCode.Q), // META + Q on Mac closes the window so probably not a great idea
			BindingCreator.OSBinding.from(OSUtil.MAC, Binding.from(KeyCode.META, KeyCode.A))
	).buildKeyBindingForCurrentOS();
	/**
	 * Increase editor font size.
	 */
	@Conf("binding.incfontsize")
	public Binding incFontSize = BindingCreator.from(
			Binding.from(KeyCode.CONTROL, KeyCode.EQUALS),
			BindingCreator.OSBinding.from(OSUtil.MAC, Binding.from(KeyCode.META, KeyCode.EQUALS))
	).buildKeyBindingForCurrentOS();
	/**
	 * Decrease editor font size.
	 */
	@Conf("binding.decfontsize")
	public Binding decFontSize = BindingCreator.from(
			Binding.from(KeyCode.CONTROL, KeyCode.MINUS),
			BindingCreator.OSBinding.from(OSUtil.MAC, Binding.from(KeyCode.META, KeyCode.MINUS))
	).buildKeyBindingForCurrentOS();

	/**
	 * Track if the user is updating a keybind, so if when they are and they hit a key that is bound,
	 * its behavior does not execute.
	 */
	private boolean isEditingBind;

	ConfKeybinding() {
		super("keybinding");
	}

	@Override
	public boolean supported(Class<?> type) {
		return type.equals(Binding.class);
	}

	/**
	 * @param isEditingBind
	 * 		New editing state.
	 */
	public void setIsUpdating(boolean isEditingBind) {
		this.isEditingBind = isEditingBind;
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
		// Ignore bind behaviors while editing them
		if (isEditingBind)
			return;
		if(!main && closeWindow.match(e) && !isEditingBind)
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
	 * @author Matt Coley
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

	/**
	 * Keybinding creator for creating different binding in different OS's.
	 *
	 * @author TimmyOVO
	 */
	public static final class BindingCreator {
		private final Binding defaultBinding;
		private final Map<OSUtil, Binding> bindings;

		private BindingCreator(Binding defaultBinding, OSBinding... osBindings) {
			this.defaultBinding = defaultBinding;
			this.bindings = Arrays.stream(OSUtil.values())
					.collect(Collectors.toMap(os -> os, os -> defaultBinding));
			this.bindings.putAll(
					Arrays.stream(osBindings)
							.collect(Collectors.toMap(
									osBinding -> osBinding.os,
									osBinding -> osBinding.binding
							))
			);
		}

		/**
		 * Build a KeybindingCreator to include all os specified keybinding.
		 *
		 * @param defaultBinding defaultBinding
		 *                       If osBindings is empty, all os's keybinding will be the same.
		 * @param osBindings     os specified keybinding.
		 * @return A KeybindingCreator instance.
		 */
		public static BindingCreator from(Binding defaultBinding, OSBinding... osBindings) {
			return new BindingCreator(defaultBinding, osBindings);
		}

		/**
		 * Match keybinding for current using os.
		 *
		 * @return A Binding instance.
		 */
		public Binding buildKeyBindingForCurrentOS() {
			return bindings.getOrDefault(OSUtil.getOSType(), defaultBinding);
		}

		/**
		 * OS specified keybinding wrapper.
		 */
		public static class OSBinding {
			public OSUtil os;
			public Binding binding;

			private OSBinding(OSUtil os, Binding binding) {
				this.os = os;
				this.binding = binding;
			}

			/**
			 * Build a key binding instance for specified os.
			 *
			 * @param os      the os to be specified
			 * @param binding key binding
			 * @return the instance of OSBinding.
			 */
			public static OSBinding from(OSUtil os, Binding binding) {
				return new OSBinding(os, binding);
			}
		}
	}
}
