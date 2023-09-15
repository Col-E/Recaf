package software.coley.recaf.services.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.AndroidClassInfo;

/**
 * Query targeting {@link AndroidClassInfo}.
 *
 * @author Matt Coley
 */
public interface AndroidClassQuery extends Query {
	@Nonnull
	AndroidClassSearchVisitor visitor(@Nullable AndroidClassSearchVisitor delegate);
}
