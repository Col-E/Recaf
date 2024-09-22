package software.coley.recaf.services.workspace.patch;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.error.Error;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.workspace.patch.model.WorkspacePatch;

import java.util.List;

/**
 * System for receiving error notifications when attempting to
 * {@link PatchApplier#apply(WorkspacePatch, PatchFeedback) apply patches}.
 *
 * @author Matt Coley
 */
public interface PatchFeedback {
	/**
	 * Called when a {@link WorkspacePatch#jvmAssemblerPatches()} could not be applied.
	 *
	 * @param errors
	 * 		Assembler errors observed.
	 */
	default void onAssemblerErrorsObserved(@Nonnull List<Error> errors) {}

	/**
	 * Called when a required path in a {@link WorkspacePatch} did not contain all necessary components.
	 *
	 * @param path
	 * 		Incomplete path.
	 */
	default void onIncompletePathObserved(@Nonnull PathNode<?> path) {}
}
