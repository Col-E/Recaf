package software.coley.recaf.util.kotlin.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.kotlin.KotlinMetadata;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Model of a constructor derived from {@link KotlinMetadata}.
 *
 * @author Matt Coley
 */
public class KtConstructor extends KtFunction {
	private final String className;

	/**
	 * @param className
	 * 		Name of class being constructed.
	 * @param parameters
	 * 		Constructor's parameters.
	 */
	public KtConstructor(@Nullable String className, @Nonnull List<KtVariable> parameters) {
		super("<init>", KtType.VOID, parameters);
		this.className = className;
	}

	@Override
	public String toString() {
		return StringUtil.shortenPath(Objects.requireNonNullElse(className, "?")) +
				"(" + getParameters().stream().map(KtVariable::toString).collect(Collectors.joining(", ")) + ")";
	}
}
