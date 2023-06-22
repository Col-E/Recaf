package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Base icon provider factory.
 *
 * @author Matt Coley
 * @see ClassIconProviderFactory For {@link JvmClassInfo} and {@link AndroidClassBundle} entries.
 * @see InnerClassIconProviderFactory For {@link InnerClassInfo} entries.
 * @see FieldIconProviderFactory For {@link FieldMember} entries.
 * @see MethodIconProviderFactory For {@link MethodMember} entries.
 * @see AnnotationIconProviderFactory Fpr {@link AnnotationInfo} entries.
 * @see FileIconProviderFactory For {@link FileInfo} entries.
 * @see DirectoryIconProviderFactory For directory entries, not linked to a specific {@link FileInfo}.
 * @see PackageIconProviderFactory  For package entries, not linked to a specific {@link ClassInfo}.
 * @see BundleIconProviderFactory For {@link Bundle} entries.
 * @see ResourceIconProviderFactory For {@link WorkspaceResource} entries.
 */
public interface IconProviderFactory {
	/**
	 * @return Icon provider that provides {@code null}.
	 */
	@Nonnull
	default IconProvider emptyProvider() {
		return () -> null;
	}
}
