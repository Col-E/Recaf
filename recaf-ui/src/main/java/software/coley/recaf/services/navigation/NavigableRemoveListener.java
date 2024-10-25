package software.coley.recaf.services.navigation;

import jakarta.annotation.Nonnull;

/**
 * Listener to observe {@link Navigable} components being removed from the UI.
 *
 * @author Matt Coley
 */
public interface NavigableRemoveListener {
	/**
	 * Called when the {@link NavigationManager} observes a {@link Navigable} is removed from the UI.
	 * <p/>
	 * Be wary that the same {@link Navigable} can technically be removed and added to the UI multiple times.
	 *
	 * @param navigable
	 * 		Removed navigable component.
	 */
	void onRemove(@Nonnull Navigable navigable);
}
