package software.coley.recaf.ui.control.richtext.suggest.java.providers;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.ui.control.richtext.suggest.java.CompletionKind;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletion;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionFactory;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionSession;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaLexicalContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider for package completions.
 *
 * @author Matt Coley
 */
public final class PackageCompletionProvider implements JavaCompletionProvider {
	@Nonnull
	@Override
	public List<JavaCompletion> complete(@Nonnull JavaCompletionSession session, @Nonnull JavaLexicalContext context) {
		Map<String, JavaCompletion> completions = new LinkedHashMap<>();
		String partial = context.partialText();
		String parentPackage = parentName(partial);
		String suffix = leafName(partial);

		for (String packageName : session.typeIndex().childPackages(parentPackage)) {
			String leaf = leafName(packageName);
			if (!JavaCompletionFactory.matchesPrefix(leaf, suffix))
				continue;
			DirectoryPathNode packagePath = session.workspace().findPackage(packageName);
			JavaCompletion.addOrReplace(completions, new JavaCompletion(
					CompletionKind.PACKAGE,
					packageName,
					packageName,
					JavaCompletionFactory.prefixPenalty(leaf, suffix),
					packagePath,
					packageName,
					0,
					""
			));
		}

		return new ArrayList<>(completions.values());
	}

	@Nonnull
	private static String parentName(@Nonnull String name) {
		int index = name.lastIndexOf('.');
		return index < 0 ? "" : name.substring(0, index);
	}

	@Nonnull
	private static String leafName(@Nonnull String name) {
		int index = name.lastIndexOf('.');
		return index < 0 ? name : name.substring(index + 1);
	}
}
