package software.coley.recaf.util.kotlin.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.kotlin.KotlinMetadata;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Model of a type reference derived from {@link KotlinMetadata}.
 *
 * @author Matt Coley
 */
public class KtType {
	public static final KtType VOID = new KtType("void", Collections.emptyList(), KtNullability.NONNULL);
	private final String name;
	private final List<KtType> arguments;
	private final KtNullability nullability;

	/**
	 * @param name
	 * 		Internal name of the type.
	 * @param arguments
	 * 		Generic type arguments.
	 * @param nullability
	 * 		Nullability state of the type in this usage context.
	 */
	public KtType(@Nonnull String name, @Nonnull List<KtType> arguments, @Nonnull KtNullability nullability) {
		this.name = name;
		this.arguments = arguments;
		this.nullability = nullability;
	}

	@Nonnull
	public static String toDescriptor(@Nullable KtVariable variable) {
		return toDescriptor(variable == null ? null : variable.getType());
	}

	@Nonnull
	public static String toDescriptor(@Nullable KtFunction function) {
		if (function == null)
			return "()Ljava/lang/Object;";
		String args = function.getParameters().stream()
				.map(KtType::toDescriptor)
				.collect(Collectors.joining());
		String ret = toDescriptor(function.getReturnType());
		return "(" + args + ")" + ret;
	}

	@Nonnull
	public static String toDescriptor(@Nullable KtType type) {
		if (type == null)
			return Types.OBJECT_TYPE.getDescriptor();

		String name = type.name;
		return "L" + name + ";";
	}

	/**
	 * @return Internal name of the type.
	 */
	@Nonnull
	public String getName() {
		return name;
	}

	/**
	 * @return Generic type arguments.
	 */
	@Nullable
	public List<KtType> getArguments() {
		return arguments;
	}

	/**
	 * @return Nullability state of the type in this usage context.
	 */
	@Nonnull
	public KtNullability getNullability() {
		return nullability;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		KtType that = (KtType) o;

		if (!name.equals(that.name)) return false;
		if (!arguments.equals(that.arguments)) return false;
		return nullability == that.nullability;
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + arguments.hashCode();
		result = 31 * result + nullability.hashCode();
		return result;
	}

	@Override
	public String toString() {
		String nullabilitySuffix = nullability == KtNullability.NULLABLE ? "?" : "";
		if (arguments.isEmpty())
			return StringUtil.shortenPath(name) + nullabilitySuffix;
		return StringUtil.shortenPath(name) + "<" + arguments.stream().map(KtType::toString).collect(Collectors.joining(", ")) + ">" + nullabilitySuffix;
	}
}
