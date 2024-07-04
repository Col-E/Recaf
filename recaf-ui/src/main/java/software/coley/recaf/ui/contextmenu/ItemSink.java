package software.coley.recaf.ui.contextmenu;

import jakarta.annotation.Nonnull;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.Ikon;
import software.coley.collections.Lists;
import software.coley.recaf.services.cell.context.ContextSource;
import software.coley.recaf.util.Lang;

import java.util.List;

import static software.coley.recaf.util.Menus.menu;

/**
 * Delegate to adding {@link MenuItem} to a {@link Menu} with some condition checking.
 *
 * @param target
 * 		Target menu items list.
 * @param source
 * 		Context source of the menu.
 *
 * @author Matt Coley
 */
public record ItemSink(@Nonnull List<MenuItem> target, @Nonnull ContextSource source) {
	/**
	 * @param key
	 * 		ID of the item to add. Doubles as {@link Lang#get(String)} key.
	 * @param item
	 * 		Menu item to add.
	 *
	 * @return {@code true} when added. {@code false} when denied by the {@link #source()}.
	 */
	public boolean add(@Nonnull String key, @Nonnull MenuItem item) {
		if (source.allow(key)) {
			target.add(item);
			return true;
		}
		return false;
	}

	/**
	 * @param key
	 * 		ID of the menu to add. Doubles as {@link Lang#get(String)} key.
	 * @param icon
	 * 		Icon of the menu to add.
	 *
	 * @return New sink to target the newly made sub-menu.
	 */
	@Nonnull
	public ItemSink withMenu(@Nonnull String key, @Nonnull Ikon icon) {
		if (source.allow(key)) {
			Menu menu = menu(key, icon);
			target.add(menu);
			return new ItemSink(menu.getItems(), source);
		}

		// Not allowed, create dummy that never appends anything.
		return new ItemSink(Lists.noopList(), source);
	}
}
