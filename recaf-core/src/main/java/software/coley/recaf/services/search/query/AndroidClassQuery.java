package software.coley.recaf.services.search.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.services.search.AndroidClassSearchVisitor;

/**
 * Query targeting {@link AndroidClassInfo}.
 *
 * @author Matt Coley
 */
public interface AndroidClassQuery extends Query {
	@Nonnull
	AndroidClassSearchVisitor visitor(@Nullable AndroidClassSearchVisitor delegate);
}
