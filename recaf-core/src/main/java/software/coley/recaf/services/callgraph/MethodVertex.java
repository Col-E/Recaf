package software.coley.recaf.services.callgraph;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.member.MethodMember;

import java.util.Collection;

/**
 * Outline of a method vertex.
 *
 * @author Amejonah
 */
public interface MethodVertex {
	/**
	 * @return Basic method details.
	 */
	@Nonnull
	MethodRef getMethod();

	/**
	 * @return Declaration of the method. Only known if the method vertex has been resolved.
	 */
	@Nullable
	MethodMember getResolvedMethod();

	/**
	 * @return Methods that call this method.
	 */
	@Nonnull
	Collection<MethodVertex> getCallers();

	/**
	 * @return Methods this method calls.
	 */
	@Nonnull
	Collection<MethodVertex> getCalls();
}
