package software.coley.recaf.util.kotlin.model;

import jakarta.annotation.Nullable;
import software.coley.recaf.util.kotlin.KotlinMetadata;

/**
 * Model of a class property derived from {@link KotlinMetadata}.
 *
 * @author Matt Coley
 */
public class KtProperty extends KtVariable {
	/**
	 * @param name
	 * 		Property name.
	 * @param type
	 * 		Property type.
	 */
	public KtProperty(@Nullable String name, @Nullable KtType type) {
		super(name, type);
	}
}
