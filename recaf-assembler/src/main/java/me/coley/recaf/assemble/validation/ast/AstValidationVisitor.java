package me.coley.recaf.assemble.validation.ast;

import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.validation.ValidationVisitor;

/**
 * Visitor impl for {@link AstValidator}.
 *
 * @author Matt Coley
 */
public interface AstValidationVisitor extends ValidationVisitor<AstValidator, AstException> {
}
