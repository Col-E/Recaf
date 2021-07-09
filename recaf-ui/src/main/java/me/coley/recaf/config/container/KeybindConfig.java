package me.coley.recaf.config.container;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.OperatingSystem;
import static me.coley.recaf.util.OperatingSystem.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Config container for keybindings.
 *
 * @author Matt Coley
 */
public class KeybindConfig implements ConfigContainer {
	/**
	 * Close current tab.
	 */
	@Group("navigation")
	@ConfigID("closetab")
	public Binding closeTab = BindingCreator.from(
			Binding.from(KeyCode.CONTROL, KeyCode.W),
			BindingCreator.OSBinding.from(MAC, Binding.from(KeyCode.META, KeyCode.W))
	).buildKeyBindingForCurrentOS();

	/**
	 * Close current tab.
	 */
	@Group("navigation")
	@ConfigID("fullscreen")
	public Binding fullscreen = Binding.from(KeyCode.F11);

	@Override
	public String displayName() {
		return Lang.get(internalName());
	}

	@Override
	public String internalName() {
		return "conf.binding";
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
		private final Map<OperatingSystem, Binding> bindings;

		private BindingCreator(Binding defaultBinding, OSBinding... osBindings) {
			this.defaultBinding = defaultBinding;
			this.bindings = Arrays.stream(OperatingSystem.values())
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
			return bindings.getOrDefault(OperatingSystem.get(), defaultBinding);
		}

		/**
		 * OS specified keybinding wrapper.
		 */
		public static class OSBinding {
			public OperatingSystem os;
			public Binding binding;

			private OSBinding(OperatingSystem os, Binding binding) {
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
			public static OSBinding from(OperatingSystem os, Binding binding) {
				return new OSBinding(os, binding);
			}
		}
	}
}
