package software.coley.recaf.services.mapping.gen.naming;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Name generator provider for {@link IncrementingNameGenerator}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class IncrementingNameGeneratorProvider extends AbstractNameGeneratorProvider<IncrementingNameGenerator> {
	public static final String ID = "incrementing";

	@Inject
	public IncrementingNameGeneratorProvider() {
		super(ID);
	}

	@Nonnull
	@Override
	public IncrementingNameGenerator createGenerator() {
		return new IncrementingNameGenerator();
	}
}
