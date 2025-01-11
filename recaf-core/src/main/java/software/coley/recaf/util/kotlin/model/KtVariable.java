package software.coley.recaf.util.kotlin.model;

import jakarta.annotation.Nullable;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.kotlin.KotlinMetadata;

import java.util.Objects;

/**
 * Model of a variable derived from {@link KotlinMetadata}.
 *
 * @author Matt Coley
 */
public non-sealed class KtVariable implements KtElement {
	private final String name;
	private final KtType type;

	/**
	 * @param name
	 * 		Variable name.
	 * @param type
	 * 		Variable type.
	 */
	public KtVariable(@Nullable String name, @Nullable KtType type) {
		this.name = name;
		this.type = type;
	}

	/**
	 * @return Variable name.
	 */
	@Nullable
	public String getName() {
		return name;
	}

	/**
	 * @return Variable type.
	 */
	@Nullable
	public KtType getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		KtVariable that = (KtVariable) o;

		if (!Objects.equals(name, that.name)) return false;
		return Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (type != null ? type.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		String typeStr = StringUtil.shortenPath(Objects.requireNonNullElse(type, "?").toString());
		String nameStr = Objects.requireNonNullElse(name, "?");
		return typeStr + " " + nameStr;
	}
}
