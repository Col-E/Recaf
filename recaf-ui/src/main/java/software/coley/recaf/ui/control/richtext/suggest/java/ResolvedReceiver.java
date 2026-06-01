package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;
import software.coley.sourcesolver.resolve.entry.DescribableEntry;

/**
 * Receiver resolved from the source at the caret position.
 *
 * @param type
 * 		Type of the receiver.
 * @param mode
 * 		Receiver mode.
 *
 * @author Matt Coley
 */
public record ResolvedReceiver(@Nonnull DescribableEntry type, @Nonnull ReceiverMode mode) {}