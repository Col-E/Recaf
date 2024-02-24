package software.coley.recaf.services.phantom;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.Service;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Collection;

/**
 * Outline for phantom class generation targeting different levels of scope.
 *
 * @author Matt Coley
 */
public interface PhantomGenerator extends Service {
	/**
	 * Generates a resource containing the phantom classes necessary to compile the JVM classes in the
	 * primary resource of the given workspace.
	 * <p>
	 * Do note that this is a largely more computationally complex task than creating phantoms for
	 * {@link #createPhantomsForClasses(Workspace, Collection) one or a few classes at a time}.
	 *
	 * @param workspace
	 * 		Workspace to scan for classes with missing references.
	 *
	 * @return Resource containing generated phantoms, targeting missing references across all classes.
	 *
	 * @throws PhantomGenerationException
	 * 		When generating phantoms failed.
	 */
	@Nonnull
	GeneratedPhantomWorkspaceResource createPhantomsForWorkspace(@Nonnull Workspace workspace)
			throws PhantomGenerationException;

	/**
	 * Generates a resource containing the phantom classes necessary to compile the given classes.
	 *
	 * @param workspace
	 * 		Workspace to pull class information from.
	 * 		If a class that is required for compilation of a given class is in the workspace, then no phantom
	 * 		for it will be generated in the resulting created resource.
	 * @param classes
	 * 		Classes to scan for missing references.
	 *
	 * @return Resource containing generated phantoms, targeting only the specified classes.
	 *
	 * @throws PhantomGenerationException
	 * 		When generating phantoms failed.
	 */
	@Nonnull
	GeneratedPhantomWorkspaceResource createPhantomsForClasses(@Nonnull Workspace workspace, @Nonnull Collection<JvmClassInfo> classes)
			throws PhantomGenerationException;
}
