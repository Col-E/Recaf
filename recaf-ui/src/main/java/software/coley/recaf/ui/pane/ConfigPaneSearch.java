package software.coley.recaf.ui.pane;

import jakarta.annotation.Nonnull;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.util.Lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static software.coley.recaf.config.ConfigGroups.PACKAGE_SPLIT;
import static software.coley.recaf.config.ConfigGroups.getGroupPackages;

/**
 * Search helpers for {@link ConfigPane}.
 */
public class ConfigPaneSearch {
	private ConfigPaneSearch() {}

	/**
	 * @param container
	 * 		Config container to check.
	 * @param query
	 * 		Query text.
	 *
	 * @return {@code true} when all query tokens match the container's searchable text.
	 */
	protected static boolean matches(@Nonnull ConfigContainer container, @Nonnull String query) {
		return matches(container, tokenizeQuery(query));
	}

	/**
	 * @param text
	 * 		Query text.
	 *
	 * @return Lower-cased, whitespace-split search tokens.
	 */
	@Nonnull
	protected static List<String> tokenizeQuery(@Nonnull String text) {
		return Arrays.stream(text.toLowerCase().trim().split("\\s+"))
				.filter(token -> !token.isBlank())
				.toList();
	}

	/**
	 * @param container
	 * 		Config container to check.
	 * @param tokens
	 * 		Query tokens.
	 *
	 * @return {@code true} when all tokens match the container's searchable text.
	 */
	protected static boolean matches(@Nonnull ConfigContainer container, @Nonnull List<String> tokens) {
		if (tokens.isEmpty())
			return true;

		String searchText = buildSearchText(container);
		for (String token : tokens)
			if (!searchText.contains(token))
				return false;

		return true;
	}

	/**
	 * @param container
	 * 		Config container to build searchable text for.
	 *
	 * @return Lower-cased search corpus for the given config container.
	 */
	@Nonnull
	protected static String buildSearchText(@Nonnull ConfigContainer container) {
		List<String> parts = new ArrayList<>();

		// Add group packages as search tokens.
		String currentPackage = null;
		for (String packageName : getGroupPackages(container)) {
			currentPackage = currentPackage == null ? packageName : currentPackage + PACKAGE_SPLIT + packageName;
			addTranslatedOrLiteral(parts, currentPackage, packageName);
		}

		// Add container ID as a search token.
		// For 3rd party configs, we only add the ID and not the group.
		boolean isThirdPartyConfig = ConfigGroups.EXTERNAL.equals(container.getGroup());
		if (isThirdPartyConfig) {
			parts.add(container.getId());
		} else {
			addTranslatedOrLiteral(parts, container.getGroupAndId(), container.getId());
		}

		// Add translated or literal value IDs as search tokens.
		for (ConfigValue<?> value : container.getValues().values()) {
			// Skip things that are hidden in the UI.
			if (value.isHidden())
				continue;

			// TODO: There is no way for plugins to provide translations for their config values.
			//  We should add support for this at some point, but for now we just add the literal ID as a search token.
			//   - https://github.com/Col-E/Recaf/issues/874
			if (isThirdPartyConfig) {
				parts.add(value.getId());
			} else {
				addTranslatedOrLiteral(parts, container.getScopedId(value), value.getId());
			}
		}

		return String.join("\n", parts).toLowerCase();
	}

	private static void addTranslatedOrLiteral(@Nonnull List<String> parts, @Nonnull String translationKey, @Nonnull String fallback) {
		if (Lang.has(translationKey))
			parts.add(Lang.get(translationKey));
		else
			parts.add(fallback);
	}
}
