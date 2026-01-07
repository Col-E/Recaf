package software.coley.recaf.ui.config;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.PlatformType;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Keybinding creator for creating different binding in different OS's.
 *
 * @author TimmyOVO
 */
public final class BindingCreator {
	private final Binding defaultBinding;
	private final Map<PlatformType, Binding> osBindings;

	private BindingCreator(@Nonnull Binding defaultBinding, OSBinding... osBindings) {
		this.defaultBinding = defaultBinding;
		this.osBindings = Arrays.stream(PlatformType.values())
				.collect(Collectors.toMap(Function.identity(), os -> defaultBinding));
		this.osBindings.putAll(
				Arrays.stream(osBindings)
						.collect(Collectors.toMap(
								osBinding -> osBinding.platform,
								osBinding -> osBinding.binding
						))
		);
	}

	/**
	 * Build a KeybindingCreator to include all os specified keybinding.
	 *
	 * @param defaultBinding
	 * 		If osBindings is empty, all os's keybinding will be the same.
	 * @param osBindings
	 * 		OS specified keybinding.
	 *
	 * @return A KeybindingCreator instance.
	 */
	@Nonnull
	public static BindingCreator bindings(@Nonnull Binding defaultBinding, OSBinding... osBindings) {
		return new BindingCreator(defaultBinding, osBindings);
	}

	/**
	 * Match keybinding for current using os.
	 *
	 * @return A Binding instance.
	 */
	@Nonnull
	public Binding buildKeyBindingForCurrentOS() {
		return osBindings.getOrDefault(PlatformType.get(), defaultBinding);
	}

	/**
	 * OS specified keybinding wrapper.
	 */
	public static class OSBinding {
		public PlatformType platform;
		public Binding binding;

		private OSBinding(@Nonnull PlatformType platform, @Nonnull Binding binding) {
			this.platform = platform;
			this.binding = binding;
		}

		/**
		 * Build a key binding instance for specified os.
		 *
		 * @param platform
		 * 		The platform to be specified.
		 * @param binding
		 * 		Key binding.
		 *
		 * @return the instance of OSBinding.
		 */
		@Nonnull
		public static OSBinding newOsBind(@Nonnull PlatformType platform, @Nonnull Binding binding) {
			return new OSBinding(platform, binding);
		}
	}
}
