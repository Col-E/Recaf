package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator;

import java.util.Comparator;

/**
 * Outline of a named snippet of code for {@link SnippetManager}.
 *
 * @param name
 * 		Snippet name / title.
 * @param description
 * 		Description of the snippets content.
 * @param content
 * 		Snippet content.
 * @param target
 * 		Assembler target the snippet is written for.
 *
 * @author Matt Coley
 */
public record Snippet(@Nonnull String name, @Nonnull String description, @Nonnull String content,
                      @Nullable SnippetTarget target) {
	/**
	 * Missing target values in older saved snippets are interpreted as JVM snippets.
	 */
	public Snippet {
		if (target == null)
			target = SnippetTarget.JVM;
	}

	@Nonnull
	@Override
	public SnippetTarget target() {
		// TODO: After a while so that people can have the configs migrated:
		//  - remove this override,
		//  - the compatibility constructor
		//  - make 'target' param @Nonnull.
		//  - Added October 2026, so same time next year is probably fine.
		return target == null ? SnippetTarget.JVM : target;
	}

	/**
	 * Shared comparator for snippets by name.
	 */
	public static final Comparator<Snippet> NAME_COMPARATOR = (a, b) -> CaseInsensitiveSimpleNaturalComparator.getInstance().compare(a.name(), b.name());

	/**
	 * @param newContent
	 * 		New content.
	 *
	 * @return A copy of this snippet with different content specified.
	 */
	@Nonnull
	public Snippet withContent(@Nonnull String newContent) {
		return new Snippet(name(), description(), newContent, target());
	}
}
