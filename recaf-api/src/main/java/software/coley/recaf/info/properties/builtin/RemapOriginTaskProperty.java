package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.services.mapping.MappingApplier;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.workspace.model.resource.ResourceAndroidClassListener;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;

/**
 * Built in property associating the creation of a {@link ClassInfo} with a
 * mapping operation. This can be used to check if a class, when received in a listener
 * such as {@link ResourceJvmClassListener} or {@link ResourceAndroidClassListener} has
 * been created as a result of usage of {@link MappingApplier}.
 *
 * @author Matt Coley
 */
public class RemapOriginTaskProperty extends BasicProperty<MappingResults> {
	public static final String KEY = "remap-origin-task";

	/**
	 * @param value
	 * 		Property value.
	 */
	public RemapOriginTaskProperty(@Nonnull MappingResults value) {
		super(KEY, value);
	}

	@Override
	public boolean persistent() {
		return false;
	}
}
