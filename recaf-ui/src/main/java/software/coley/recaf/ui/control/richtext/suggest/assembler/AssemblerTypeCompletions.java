package software.coley.recaf.ui.control.richtext.suggest.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.util.EscapeUtil;
import software.coley.recaf.ui.control.richtext.suggest.lookups.TypeVisibility;

import java.util.List;
import java.util.Objects;

/**
 * Class type completions, offered as either an internal name or a descriptor depending on the typed prefix.
 *
 * @author Matt Coley
 */
final class AssemblerTypeCompletions {
	private AssemblerTypeCompletions() {}

	/**
	 * @param scope
	 * 		Workspace state for the completion request.
	 * @param prefix
	 * 		Typed text. A leading {@code [} or {@code L} selects descriptor form.
	 * @param descriptorByDefault
	 *        {@code true} to offer descriptor form even when the prefix does not select it.
	 * @param requireArray
	 *        {@code true} to only offer array types, even when the prefix does not select it.
	 *
	 * @return Class completions whose text extends the prefix, limited to types accessible from the edited class.
	 */
	@Nonnull
	static List<AssemblerCompletion> complete(@Nonnull AssemblerCompletionScope scope, @Nonnull String prefix,
	                                          boolean descriptorByDefault, boolean requireArray) {
		String internalPrefix = classNamePrefix(prefix);
		return scope.typesWithPrefix(internalPrefix).stream()
				.filter(candidate -> TypeVisibility.isAccessibleType(candidate, scope.currentPackage()))
				.map(candidate -> {
					String text = classTypeText(candidate.internalName(), prefix, descriptorByDefault, requireArray);
					return new AssemblerCompletion.Type(Objects.requireNonNull(candidate.path()),
							EscapeUtil.escapeLiteral(text));
				})
				.filter(type -> type.text().startsWith(prefix) && !type.text().equals(prefix))
				.map(AssemblerCompletion.class::cast)
				.toList();
	}

	/**
	 * @param prefix
	 * 		Typed text, possibly with array dimensions, a leading {@code L}, and a trailing {@code ;}.
	 *
	 * @return The internal class name the prefix refers to, with descriptor syntax removed.
	 */
	@Nonnull
	static String classNamePrefix(@Nonnull String prefix) {
		int index = 0;
		while (index < prefix.length() && prefix.charAt(index) == '[')
			index++;
		if (index < prefix.length() && prefix.charAt(index) == 'L')
			index++;
		String name = prefix.substring(index);
		return name.endsWith(";") ? name.substring(0, name.length() - 1) : name;
	}

	/**
	 * @param name
	 * 		Internal class name.
	 * @param typedPrefix
	 * 		Typed text, used to tell whether the user is writing a descriptor.
	 * @param descriptorByDefault
	 *        {@code true} to produce descriptor form even when the typed prefix does not select it.
	 * @param requireArray
	 *        {@code true} to produce array form even when the typed prefix does not select it.
	 *
	 * @return The class name in the form the user is typing.
	 */
	@Nonnull
	static String classTypeText(@Nonnull String name, @Nonnull String typedPrefix,
	                            boolean descriptorByDefault, boolean requireArray) {
		int arrayDimensions = 0;
		while (arrayDimensions < typedPrefix.length() && typedPrefix.charAt(arrayDimensions) == '[')
			arrayDimensions++;
		boolean descriptor = descriptorByDefault || arrayDimensions > 0
				|| arrayDimensions < typedPrefix.length() && typedPrefix.charAt(arrayDimensions) == 'L';
		if (requireArray && arrayDimensions == 0)
			arrayDimensions = 1;
		if (!descriptor && !requireArray)
			return name;
		return "[".repeat(arrayDimensions) + "L" + name + ";";
	}
}
