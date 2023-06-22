package software.coley.recaf.services.mapping.gen.generator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.mapping.gen.NameGenerator;
import software.coley.recaf.util.StringUtil;

/**
 * Basic name generator using a given alphabet of characters to generate pseudo-random names with.
 * Names will always yield the same value for the same input.
 *
 * @author Matt Coley
 */
public class AlphabetNameGenerator implements NameGenerator {
	private final String alphabet;
	private final int length;

	/**
	 * @param alphabet
	 * 		Alphabet to use.
	 * @param length
	 * 		Length of output names.
	 */
	public AlphabetNameGenerator(@Nonnull String alphabet, int length) {
		this.alphabet = alphabet;
		this.length = length;
	}

	@Nonnull
	private String name(@Nullable String original) {
		int seed = original == null ? alphabet.hashCode() : original.hashCode();
		return StringUtil.generateName(alphabet, length, seed);
	}

	@Nonnull
	@Override
	public String mapClass(@Nonnull ClassInfo info) {
		if (info.isInDefaultPackage())
			return name(info.getName());

		// Ensure classes in the same package are kept together
		return name(info.getPackageName()) + "/" + name(info.getName());
	}

	@Nonnull
	@Override
	public String mapField(@Nonnull ClassInfo owner, @Nonnull FieldMember field) {
		return name(owner.getName() + "#" + field.getName());
	}

	@Nonnull
	@Override
	public String mapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {
		return name(owner.getName() + "#" + method.getName());
	}
}
