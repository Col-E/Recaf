package software.coley.recaf.ui.config;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wrapper to act as bind utility.
 *
 * @author Matt Coley
 */
public class Binding extends ArrayList<String> {
	private final String id;

	/**
	 * @param id
	 * 		Unique identifier of the binding.
	 */
	public Binding(@Nonnull String id) {
		this.id = id;
	}

	/**
	 * @return Unique identifier of the binding.
	 */
	@Nonnull
	public String getId() {
		return id;
	}

	/**
	 * @param original
	 * 		Original key to replace.
	 * @param replacement
	 * 		Replacement key.
	 *
	 * @return Copy of the binding with the given key replaced.
	 */
	@Nonnull
	public Binding withReplacement(@Nonnull KeyCode original, @Nonnull KeyCode replacement) {
		Binding copy = new Binding(id);
		String toMatch = original.getName().toLowerCase();
		String toReplace = replacement.getName().toLowerCase();
		for (String item : this) {
			if (item.equals(toMatch))
				copy.add(toReplace);
			else
				copy.add(item);
		}
		return copy;
	}

	/**
	 * @param id
	 * 		Unique identifier of the binding.
	 * @param codesString
	 * 		Series of keys for a keybind, split by '+'.
	 * @param mask
	 * 		Optional mask to include.
	 *
	 * @return Binding from keys + mask.
	 */
	@Nonnull
	public static Binding newBind(@Nonnull String id, @Nonnull String codesString, @Nullable KeyCode mask) {
		String[] codes = codesString.split("\\+");
		Stream<String> stream = Arrays.stream(codes);
		if (mask != null)
			stream = Stream.concat(Stream.of(mask.getName()), stream);
		return stream.map(String::toLowerCase).collect(Collectors.toCollection(() -> new Binding(id)));
	}

	/**
	 * @param id
	 * 		Unique identifier of the binding.
	 * @param event
	 * 		Key event.
	 *
	 * @return Binding from keys of the event.
	 */
	@Nonnull
	public static Binding newBind(@Nonnull String id, @Nonnull KeyEvent event) {
		return newBind(id, namesOf(event));
	}

	/**
	 * @param id
	 * 		Unique identifier of the binding.
	 * @param codes
	 * 		Series of JFX {@link KeyCode} for a keybind. Unlike {@link #newBind(String, String, KeyCode)}
	 * 		it is implied that the mask is given in this series, if one is intended.
	 *
	 * @return Binding from keys.
	 */
	@Nonnull
	public static Binding newBind(@Nonnull String id, @Nonnull KeyCode... codes) {
		return Arrays.stream(codes).map(KeyCode::getName)
				.map(String::toLowerCase)
				.collect(Collectors.toCollection(() -> new Binding(id)));
	}

	/**
	 * @param id
	 * 		Unique identifier of the binding.
	 * @param codes
	 * 		Series of keys for a keybind, in string representation.
	 *
	 * @return Binding from keys.
	 */
	@Nonnull
	public static Binding from(@Nonnull String id, @Nonnull Collection<String> codes) {
		return codes.stream()
				.map(String::toLowerCase)
				.sorted((a, b) -> (a.length() > b.length()) ? -1 : a.compareTo(b))
				.collect(Collectors.toCollection(() -> new Binding(id)));
	}

	/**
	 * @param id
	 * 		Unique identifier of the binding.
	 * @param codes
	 * 		Series of keys for a keybind.
	 *
	 * @return Binding from keys.
	 */
	@Nonnull
	public static Binding newBind(@Nonnull String id, @Nonnull Collection<String> codes) {
		return codes.stream()
				.map(String::toLowerCase)
				.sorted((a, b) -> (a.length() > b.length()) ? -1 : a.compareTo(b))
				.collect(Collectors.toCollection(() -> new Binding(id)));
	}

	@Override
	public String toString() {
		return String.join(" + ", this);
	}

	/**
	 * @param event
	 * 		JFX key event.
	 *
	 * @return {@code true} if the event matches the current bind.
	 */
	public boolean match(@Nonnull KeyEvent event) {
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

	/**
	 * @param event
	 * 		Event to get key names of <i>(Key name plus mask name)</i>.
	 *
	 * @return Key names of the event.
	 */
	@Nonnull
	public static Set<String> namesOf(@Nonnull KeyEvent event) {
		Set<String> eventSet = new LinkedHashSet<>();
		if (event.isControlDown())
			eventSet.add(nameOf(KeyCode.CONTROL));
		if (event.isMetaDown())
			eventSet.add(nameOf(KeyCode.META));
		if (event.isAltDown())
			eventSet.add(nameOf(KeyCode.ALT));
		if (event.isShiftDown())
			eventSet.add(nameOf(KeyCode.SHIFT));
		eventSet.add(nameOf(event.getCode()));
		return eventSet;
	}

	/**
	 * @param code
	 * 		Key code to get name of.
	 *
	 * @return Key name of the event.
	 */
	@Nonnull
	public static String nameOf(KeyCode code) {
		return code.getName().toLowerCase();
	}
}
