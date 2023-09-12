package software.coley.recaf.services.mapping.gen.generator;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.services.mapping.gen.NameGeneratorProvider;

/**
 * Name generator provider for {@link IncrementingNameGenerator}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class IncrementingNameGeneratorProvider extends BasicConfigContainer implements NameGeneratorProvider<IncrementingNameGenerator> {
	public static final String ID = "incrementing";

	@Inject
	public IncrementingNameGeneratorProvider() {
		super(GROUP_ID, ID);
	}

	@Nonnull
	@Override
	public IncrementingNameGenerator createGenerator() {
		return new IncrementingNameGenerator();
	}
}
