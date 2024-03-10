package software.coley.recaf.services.search.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.search.JvmClassSearchVisitor;

/**
 * Query targeting {@link JvmClassInfo}.
 *
 * @author Matt Coley
 */
public interface JvmClassQuery extends Query {
	@Nonnull
	JvmClassSearchVisitor visitor(@Nullable JvmClassSearchVisitor delegate);
}
