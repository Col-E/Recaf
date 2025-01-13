package software.coley.recaf.ui.config;

import com.google.gson.JsonElement;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableMap;
import software.coley.recaf.config.*;
import software.coley.recaf.services.config.ConfigComponentManager;
import software.coley.recaf.services.config.TypedConfigComponentFactory;
import software.coley.recaf.services.json.GsonProvider;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.pane.editing.ClassPane;
import software.coley.recaf.ui.pane.editing.FilePane;
import software.coley.recaf.ui.pane.editing.jvm.JvmDecompilerPane;
import software.coley.recaf.util.Lang;
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
	private static final String ID_GOTO = "editor.goto";
	private final BindingBundle bundle;

	@Inject
	public KeybindingConfig(@Nonnull GsonProvider gsonProvider, @Nonnull ConfigComponentManager componentManager) {
		super(ConfigGroups.SERVICE_UI, ID + CONFIG_SUFFIX);

		// We will only be storing one 'value' so that the UI can treat it as a singular element.
		bundle = new BindingBundle(Arrays.asList(
				createBindForPlatform(ID_QUICK_NAV, CONTROL, G),
				createBindForPlatform(ID_FIND, CONTROL, F),
				createBindForPlatform(ID_REPLACE, CONTROL, R),
				createBindForPlatform(ID_SAVE, CONTROL, S),
				createBindForPlatform(ID_RENAME, ALT, R),
				createBindForPlatform(ID_GOTO, F3)
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

		// Register custom config component display for the binding bundle.
		componentManager.register(BindingBundle.class, new TypedConfigComponentFactory<>(true, BindingBundle.class) {
			@Nonnull
			@Override
			public Node create(@Nonnull ConfigContainer container, @Nonnull ConfigValue<BindingBundle> value) {
				GridPane grid = new GridPane();
				grid.setVgap(10);
				grid.setHgap(10);

				// Tree-map should show the items in grouped order by key.
				new TreeMap<>(bundle).forEach((key, bind) -> {
					BoundLabel label = new BoundLabel(Lang.getBinding("bind." + key));
					BindingInputField inputField = new BindingInputField(bundle, key, bind);
					grid.addRow(grid.getRowCount(), label, inputField);
				});

				return grid;
			}
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
	 * @return Keybinding for renaming whatever is found at the current caret position.
	 *
	 * @see JvmDecompilerPane Usage in decompiler.
	 */
	@Nonnull
	public Binding getGoto() {
		return Objects.requireNonNull(bundle.get(ID_GOTO));
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
		private final ObservableBoolean isEditing = new ObservableBoolean(false);

		public BindingBundle(@Nonnull List<Binding> binds) {
			super(binds.stream().collect(Collectors.toMap(Binding::getId, Function.identity())), HashMap::new);
		}

		/**
		 * Marks the bundle as being live-edited or not. Global key-binds should not be handled when editing.
		 *
		 * @param editing
		 *        {@code true} to indicate the bundle is being edited by the user.
		 *        {@code false} to indicate the user is not editing.
		 */
		public void setIsEditing(boolean editing) {
			isEditing.setValue(editing);
		}
	}

	/**
	 * Text field that updates a {@link Binding}.
	 */
	private static class BindingInputField extends TextField {
		private Binding lastState;

		private BindingInputField(@Nonnull BindingBundle bundle, @Nonnull String id, @Nonnull Binding bind) {
			getStyleClass().add("key-field");
			setText(bind.toString());
			setPromptText(Lang.get("bind.inputprompt.initial"));

			// Show prompt when focused, otherwise show current target binding text.
			focusedProperty().addListener((v, old, focus) -> setText(focus ? Lang.get("bind.inputprompt.finish") : bind.toString()));
			setFocusTraversable(false);

			// Track binding state while typing (will remember last pressed key combo, before pressing enter)
			setOnKeyPressed(e -> {
				e.consume();

				// Skip if no-name support for the character
				if (e.getCode().getName().equalsIgnoreCase("undefined"))
					return;

				// Set target to last key-press combo
				if (e.getCode() == ENTER) {
					bundle.setIsEditing(false);

					bind.clear();
					bind.addAll(lastState);
					lastState = null;

					// Drop focus so the listener will update the display text.
					getParent().requestFocus();
					return;
				} else if (e.getCode() == ESCAPE) {
					// Drop focus so the listener will update the display text.
					lastState = null;
					getParent().requestFocus();
					return;
				}

				// Update key-press combo.
				lastState = newBind(id, e);
				bundle.setIsEditing(true);
			});

			// Disable text updating
			setOnKeyTyped(Event::consume);
		}
	}
}
