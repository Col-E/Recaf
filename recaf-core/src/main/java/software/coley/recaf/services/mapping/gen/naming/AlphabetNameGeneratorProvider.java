package software.coley.recaf.services.mapping.gen.naming;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableInteger;
import software.coley.observables.ObservableString;
import software.coley.recaf.config.BasicConfigValue;

/**
 * Name generator provider for {@link AlphabetNameGenerator}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AlphabetNameGeneratorProvider extends AbstractNameGeneratorProvider<AlphabetNameGenerator> {
	public static final String ID = "alphabet";
	private final ObservableString alphabet = new ObservableString("abcdefghijklmnopqrstuvwxyz");
	private final ObservableInteger length = new ObservableInteger(3);

	@Inject
	public AlphabetNameGeneratorProvider() {
		super(ID);
		addValue(new BasicConfigValue<>("alphabet", String.class, alphabet));
		addValue(new BasicConfigValue<>("length", int.class, length));
	}

	@Nonnull
	@Override
	public AlphabetNameGenerator createGenerator() {
		return new AlphabetNameGenerator(alphabet.getValue(), length.getValue());
	}

	/**
	 * @return Alphabet of characters to use when creating names.
	 */
	@Nonnull
	public ObservableString getAlphabet() {
		return alphabet;
	}

	/**
	 * @return Length of output names.
	 */
	@Nonnull
	public ObservableInteger getLength() {
		return length;
	}
}
