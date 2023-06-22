package software.coley.recaf.services.phantom;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.Service;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collection;

/**
 * Outline for phantom class generation targeting different levels of scope.
 *
 * @author Matt Coley
 */
public interface PhantomGenerator extends Service {
	/**
	 * @param workspace
	 * 		Workspace to scan for classes with missing references.
	 *
	 * @return Resource containing generated phantoms, targeting missing references across all classes.
	 *
	 * @throws PhantomGenerationException
	 * 		When generating phantoms failed.
	 */
	@Nonnull
	WorkspaceResource createPhantomsForWorkspace(@Nonnull Workspace workspace)
			throws PhantomGenerationException;

	/**
	 * @param workspace
	 * 		Workspace to pull class information from.
	 * @param classes
	 * 		Classes to scan for missing references.
	 *
	 * @return Resource containing generated phantoms, targeting only the specified classes.
	 *
	 * @throws PhantomGenerationException
	 * 		When generating phantoms failed.
	 */
	@Nonnull
	WorkspaceResource createPhantomsForClasses(@Nonnull Workspace workspace, @Nonnull Collection<JvmClassInfo> classes)
			throws PhantomGenerationException;
}
