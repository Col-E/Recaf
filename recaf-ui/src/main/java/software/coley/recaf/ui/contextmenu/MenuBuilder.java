package software.coley.recaf.ui.contextmenu;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.Ikon;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.util.Lang;

import static software.coley.recaf.util.Menus.action;

/**
 * Base builder outline.
 *
 * @param <B>
 * 		Recursive self type.
 *
 * @author Matt Coley
 */
public abstract class MenuBuilder<B extends MenuBuilder<B>> {
	protected final ItemSink sink;
	private final B parent;

	/**
	 * @param parent
	 * 		Optional parent menu.
	 * @param sink
	 * 		Sink to append menu items with.
	 */
	protected MenuBuilder(@Nullable B parent, @Nonnull ItemSink sink) {
		this.parent = parent;
		this.sink = sink;
	}

	/**
	 * @param id
	 * 		Menu item ID. Doubles as {@link Lang#get(String)} key.
	 * @param icon
	 * 		Menu item graphic.
	 * @param action
	 * 		Menu item action.
	 *
	 * @return Handler for optional post-addition manipulations of the added item.
	 */
	@Nonnull
	public MenuHandler<MenuItem> item(@Nonnull String id, @Nonnull Ikon icon, @Nonnull Runnable action) {
		ActionMenuItem item = action(id, icon, action);
		if (sink.add(id, item))
			return consumer -> consumer.accept(item);
		return unused -> {};
	}

	/**
	 * Used to escape from a {@link #submenu(String, Ikon)} back to the parent.
	 *
	 * @return Parent builder.
	 */
	@Nullable
	public B close() {
		return parent;
	}

	/**
	 * Creates a new builder of the same type for a new sub-menu.
	 *
	 * @param key
	 * 		Menu ID. Doubles as {@link Lang#get(String)} key.
	 * @param icon
	 * 		Menu graphic.
	 *
	 * @return New menu builder of the same type, targeting the newly created menu.
	 */
	public abstract B submenu(@Nonnull String key, @Nonnull Ikon icon);
}
