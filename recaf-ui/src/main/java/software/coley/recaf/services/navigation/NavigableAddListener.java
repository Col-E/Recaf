package software.coley.recaf.services.navigation;

import jakarta.annotation.Nonnull;

/**
 * Listener to observe {@link Navigable} components being added to the UI.
 *
 * @author Matt Coley
 */
public interface NavigableAddListener {
	/**
	 * Called when the {@link NavigationManager} observes a {@link Navigable} is added to the UI.
	 * <p/>
	 * Be wary that the same {@link Navigable} can technically be removed and added to the UI multiple times.
	 *
	 * @param navigable
	 * 		Added navigable component.
	 */
	void onAdd(@Nonnull Navigable navigable);
}
