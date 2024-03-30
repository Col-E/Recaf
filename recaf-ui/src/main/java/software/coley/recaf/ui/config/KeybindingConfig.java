package software.coley.recaf.ui.config;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.input.KeyCode;
import software.coley.observables.ObservableMap;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicMapConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.json.GsonProvider;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.pane.editing.ClassPane;
import software.coley.recaf.ui.pane.editing.FilePane;
import software.coley.recaf.ui.pane.editing.jvm.JvmDecompilerPane;
import software.coley.recaf.util.PlatformType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static javafx.scene.input.KeyCode.*;
import static software.coley.recaf.ui.config.Binding.nameOf;
import static software.coley.recaf.ui.config.Binding.newBind;
import static software.coley.recaf.ui.config.BindingCreator.OSBinding.newOsBind;
import static software.coley.recaf.ui.config.BindingCreator.bindings;

/**
 * Config for various keybindings.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class KeybindingConfig extends BasicConfigContainer {
	public static final String ID = "bind";
	private static final String ID_QUICK_NAV = "quicknav";
	private static final String ID_FIND = "editor.find";
	private static final String ID_REPLACE = "editor.replace";
	private static final String ID_SAVE = "editor.save";
	private static final String ID_RENAME = "editor.rename";
	private final BindingBundle bundle;

	@Inject
	public KeybindingConfig(@Nonnull GsonProvider gsonProvider) {
		super(ConfigGroups.SERVICE_UI, ID + CONFIG_SUFFIX);

		// We will only be storing one 'value' so that the UI can treat it as a singular element.
		bundle = new BindingBundle(Arrays.asList(
				createBindForPlatform(ID_QUICK_NAV, CONTROL, G),
				createBindForPlatform(ID_FIND, CONTROL, F),
				createBindForPlatform(ID_REPLACE, CONTROL, R),
				createBindForPlatform(ID_SAVE, CONTROL, S),
				createBindForPlatform(ID_RENAME, ALT, R)
		));
		addValue(new BasicMapConfigValue<>("bundle", BindingBundle.class, String.class, Binding.class, bundle));

		// Register custom json adapter for the binding bundle type.
		gsonProvider.addTypeDeserializer(BindingBundle.class, (json, typeOfT, context) -> {
			Set<String> expected = new HashSet<>(bundle.keySet());
			Map<String, JsonElement> map = json.getAsJsonObject().asMap();
			List<Binding> bindings = new ArrayList<>(map.size());
			map.forEach((id, keysElement) -> {
				List<String> keyNames = keysElement.getAsJsonArray().asList().stream()
						.map(JsonElement::getAsString)
						.toList();
				bindings.add(newBind(id, keyNames));
				expected.remove(id);
			});

			// Fill in values from default config that do not exist in the serialized model
			expected.forEach(missingId -> bindings.add(bundle.get(missingId)));

			return new BindingBundle(bindings);
		});
	}

	/**
	 * @return Keybinding for opening the quick-nav stage.
	 */
	@Nonnull
	public Binding getQuickNav() {
		return Objects.requireNonNull(bundle.get(ID_QUICK_NAV));
	}

	/**
	 * @return Keybinding for opening find operations.
	 *
	 * @see SearchBar Used for {@link Editor}.
	 */
	@Nonnull
	public Binding getFind() {
		return Objects.requireNonNull(bundle.get(ID_FIND));
	}

	/**
	 * @return Keybinding for opening replace operations.
	 *
	 * @see SearchBar Used for {@link Editor}.
	 */
	@Nonnull
	public Binding getReplace() {
		return Objects.requireNonNull(bundle.get(ID_REPLACE));
	}

	/**
	 * @return Keybinding to save within a {@link ClassPane} or {@link FilePane}.
	 */
	@Nonnull
	public Binding getSave() {
		return Objects.requireNonNull(bundle.get(ID_SAVE));
	}

	/**
	 * @return Keybinding for renaming whatever is found at the current caret position.
	 *
	 * @see JvmDecompilerPane Usage in decompiler.
	 */
	@Nonnull
	public Binding getRename() {
		return Objects.requireNonNull(bundle.get(ID_RENAME));
	}

	/**
	 * Wrapper around {@link Binding#newBind(String, KeyCode...)} and {@link BindingCreator}
	 * to swap out {@link KeyCode#CONTROL} for {@link KeyCode#META} for Mac users.
	 *
	 * @param id
	 * 		Keybinding ID.
	 * @param codes
	 * 		Key-codes to use.
	 *
	 * @return Binding for the current platform.
	 */
	private static Binding createBindForPlatform(String id, KeyCode... codes) {
		Binding defaultBind = newBind(id, codes);
		// Swap out CONTROL for META on Mac.
		if (defaultBind.contains(nameOf(CONTROL)))
			return bindings(defaultBind, newOsBind(PlatformType.MAC, defaultBind))
					.buildKeyBindingForCurrentOS();
		return defaultBind;
	}

	/**
	 * Binding bundle containing all keys.
	 */
	public static class BindingBundle extends ObservableMap<String, Binding, Map<String, Binding>> {
		public BindingBundle(@Nonnull List<Binding> binds) {
			super(binds.stream().collect(Collectors.toMap(Binding::getId, Function.identity())), HashMap::new);
		}
	}
}
