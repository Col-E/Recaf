package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;

/**
 * Interface to use for explicit file format implementations of {@link Mappings}.
 * <br>
 * <h2>Relevant noteworthy points</h2>
 * <b>Incomplete mappings</b>: Not all mapping formats are complete in their representation. Some may omit the
 * descriptor of fields <i>(Because at the source level, overloaded names are illegal within the same class)</i>.
 * So while the methods defined here will always be provided all of this information, each implementation may have to
 * do more than a flat one-to-one lookup in these cases.
 * <br><br>
 * <b>Implementations do not need to be complete to partially work</b>: Some mapping formats do not support renaming
 * for variable names in methods. This is fine, because any method in this interface can be implemented as a no-op by
 * returning {@code null}.
 *
 * @author Matt Coley
 */
public interface MappingFileFormat {
	/**
	 * @return Name of the mapping format implementation.
	 */
	@Nonnull
	String implementationName();

	/**
	 * @param mappingsText
	 * 		Text of the mappings to parse.
	 *
	 * @return Intermediate mappings from parsed text.
	 */
	IntermediateMappings parse(@Nonnull String mappingsText);

	/**
	 * Some mapping formats do not include field types since name overloading is illegal at the source level of Java.
	 * It's valid in the bytecode but the mapping omits this info since it isn't necessary information for mapping
	 * that does not support name overloading.
	 *
	 * @return {@code true} when field mappings include the type descriptor in their lookup information.
	 */
	boolean doesSupportFieldTypeDifferentiation();

	/**
	 * Some mapping forats do not include variable types since name overloading is illegal at the source level of Java.
	 * Variable names are not used by the JVM at all so their names can be anything at the bytecode level. So including
	 * the type makes it easier to reverse mappings.
	 *
	 * @return {@code true} when variable mappings include the type descriptor in their lookup information.
	 */
	boolean doesSupportVariableTypeDifferentiation();

	/**
	 * @return {@code true} when exporting the current mappings to text is supported.
	 *
	 * @see #exportText(Mappings)
	 */
	default boolean supportsExportText() {
		return true;
	}

	/**
	 * @param mappings
	 * 		Mappings to write with the current format.
	 *
	 * @return Exported mapping text in the current format.
	 */
	default String exportText(Mappings mappings) {
		return null;
	}
}
