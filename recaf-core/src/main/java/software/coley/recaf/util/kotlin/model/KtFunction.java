package software.coley.recaf.util.kotlin.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.kotlin.KotlinMetadata;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Model of a function derived from {@link KotlinMetadata}.
 *
 * @author Matt Coley
 */
public non-sealed class KtFunction implements KtElement {
	private final String name;
	private final KtType returnType;
	private final List<KtVariable> parameters;

	/**
	 * @param name
	 * 		Function name.
	 * @param returnType
	 * 		Function's return type.
	 * @param parameters
	 * 		Function's parameters.
	 */
	public KtFunction(@Nullable String name, @Nullable KtType returnType, @Nonnull List<KtVariable> parameters) {
		this.name = name;
		this.returnType = returnType;
		this.parameters = parameters;
	}

	/**
	 * @return Function name.
	 */
	@Nullable
	public String getName() {
		return name;
	}

	/**
	 * @return Function's return type.
	 */
	@Nullable
	public KtType getReturnType() {
		return returnType;
	}

	/**
	 * @return Function's parameters.
	 */
	@Nonnull
	public List<KtVariable> getParameters() {
		return parameters;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		KtFunction that = (KtFunction) o;

		if (!Objects.equals(name, that.name)) return false;
		if (!Objects.equals(returnType, that.returnType)) return false;
		return parameters.equals(that.parameters);
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (returnType != null ? returnType.hashCode() : 0);
		result = 31 * result + parameters.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return StringUtil.shortenPath(Objects.requireNonNullElse(returnType, "?").toString()) + " "
				+ Objects.requireNonNullElse(name, "?") +
				"(" + parameters.stream().map(KtVariable::toString).collect(Collectors.joining(", ")) + ")";
	}
}
