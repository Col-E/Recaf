package software.coley.recaf.services.mapping;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Used to intercept application state before and after {@link MappingResults#apply()}.
 *
 * @author Matt Coley
 * @see MappingResults Can be added to the constuctor to affect a single mapping job.
 * @see MappingListeners Can be added in order to affect all mapping jobs.
 */
public interface MappingApplicationListener {
	/**
	 * @param workspace
	 * 		Workspace the mappings are applied to.
	 * @param mappingResults
	 * 		Mapping results to be applied.
	 */
	void onPreApply(@Nonnull Workspace workspace, @Nonnull MappingResults mappingResults);

	/**
	 * @param workspace
	 * 		Workspace the mappings are applied to.
	 * @param mappingResults
	 * 		Mapping results that were applied.
	 */
	void onPostApply(@Nonnull Workspace workspace, @Nonnull MappingResults mappingResults);
}
