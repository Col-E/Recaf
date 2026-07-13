package software.coley.recaf.services.stub;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import software.coley.recaf.util.Translatable;

import java.util.function.BiConsumer;

/**
 * A possible value to return from a stubbed method.
 *
 * @param name
 * 		Translatable display name.
 * @param action
 * 		Callback which writes the complete return instruction sequence.
 *
 * @author Matt Coley
 */
public record StubStrategy(@Nonnull Translatable name,
                           @Nonnull BiConsumer<MethodVisitor, Type> action) {}
