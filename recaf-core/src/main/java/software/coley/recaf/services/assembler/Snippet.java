package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
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
 *
 * @author Matt Coley
 */
public record Snippet(@Nonnull String name, @Nonnull String description, @Nonnull String content) {
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
		return new Snippet(name(), description(), newContent);
	}
}
