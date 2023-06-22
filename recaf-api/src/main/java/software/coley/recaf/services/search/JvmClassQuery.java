package software.coley.recaf.services.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.JvmClassInfo;

/**
 * Query targeting {@link JvmClassInfo}.
 *
 * @author Matt Coley
 */
public interface JvmClassQuery extends Query {
	@Nonnull
	JvmClassSearchVisitor visitor(@Nullable JvmClassSearchVisitor delegate);
}
