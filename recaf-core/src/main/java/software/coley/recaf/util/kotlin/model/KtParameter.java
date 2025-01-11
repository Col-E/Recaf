package software.coley.recaf.util.kotlin.model;

import jakarta.annotation.Nullable;
import software.coley.recaf.util.kotlin.KotlinMetadata;

/**
 * Model of a function parameter derived from {@link KotlinMetadata}.
 *
 * @author Matt Coley
 */
public class KtParameter extends KtVariable {
	/**
	 * @param name
	 * 		Parameter name.
	 * @param type
	 * 		Parameter type.
	 */
	public KtParameter(@Nullable String name, @Nullable KtType type) {
		super(name, type);
	}
}
