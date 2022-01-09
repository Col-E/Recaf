package me.coley.recaf.config.binds;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wrapper to act as bind utility.
 *
 * @author Matt Coley
 */
public class Binding extends ArrayList<String> {
	/**
	 * @param string
	 * 		Series of keys for a keybind, split by '+'.
	 * @param mask
	 * 		Optional mask to include.
	 *
	 * @return Binding from keys + mask.
	 */
	public static Binding newBind(String string, KeyCode mask) {
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
	public static Binding newBind(KeyEvent event) {
		return newBind(namesOf(event));
	}

	/**
	 * @param codes
	 * 		Series of JFX {@link KeyCode} for a keybind. Unlike {@link #newBind(String, KeyCode)}
	 * 		it is implied that the mask is given in this series, if one is intended.
	 *
	 * @return Binding from keys.
	 */
	public static Binding newBind(KeyCode... codes) {
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
	public static Binding newBind(Collection<String> codes) {
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
