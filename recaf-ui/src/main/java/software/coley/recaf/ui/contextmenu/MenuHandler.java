package software.coley.recaf.ui.contextmenu;

import jakarta.annotation.Nonnull;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.MenuItem;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Similar to {@link Optional} but with specific handling for {@link MenuItem} content.
 * Used as return types for additions to menus via {@link MenuBuilder} implementations.
 *
 * @param <M>
 * 		Menu content.
 *
 * @author Matt Coley
 */
public interface MenuHandler<M extends MenuItem> {
	/**
	 * @param handlers
	 * 		Multiple handlers.
	 * @param <M>
	 * 		Menu content.
	 *
	 * @return Handler to delegate an action to the given handlers.
	 */
	@Nonnull
	@SafeVarargs
	static <M extends MenuItem> MenuHandler<M> each(MenuHandler<M>... handlers) {
		return consumer -> {
			for (MenuHandler<M> handler : handlers) {
				handler.configure(consumer);
			}
		};
	}

	/**
	 * @param consumer
	 * 		Consumer to act on the added menu item.
	 */
	void configure(@Nonnull Consumer<M> consumer);

	/**
	 * Disables the target menu item if the condition is met.
	 *
	 * @param condition
	 * 		Set condition.
	 */
	default void disableWhen(boolean condition) {
		configure(i -> i.setDisable(condition));
	}

	/**
	 * Disables the target menu item if the condition is met.
	 *
	 * @param condition
	 * 		Set condition.
	 */
	default void disableWhen(@Nonnull ObservableValue<Boolean> condition) {
		configure(i -> i.disableProperty().bind(condition));
	}
}
