package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;

/**
 * Variable in scope at the caret position.
 *
 * @param name
 * 		Variable name.
 * @param parameter
 *        {@code true} when the variable is declared as a method parameter, or {@code false} when it's a local variable.
 *
 * @author Matt Coley
 */
public record ScopedVariable(@Nonnull String name, boolean parameter) {}
