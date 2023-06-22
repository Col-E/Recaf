package software.coley.recaf.ui.control.richtext.syntax;

import jakarta.annotation.Nonnull;
import org.fxmisc.richtext.model.StyleSpans;

import java.util.Collection;

/**
 * Wrapper of created style-spans and starting position.
 *
 * @author Matt Coley
 */
public record StyleResult(@Nonnull StyleSpans<Collection<String>> spans, int position) {
}
