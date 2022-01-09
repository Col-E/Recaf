package me.coley.recaf.assemble.validation.bytecode;

import me.coley.recaf.assemble.BytecodeException;
import me.coley.recaf.assemble.validation.ValidationVisitor;

/**
 * Visitor impl for {@link BytecodeValidator}.
 *
 * @author Matt Coley
 */
public interface BytecodeValidationVisitor extends ValidationVisitor<BytecodeValidator, BytecodeException> {
}
