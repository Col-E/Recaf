package software.coley.recaf.ui.pane.editing.jvm.lowlevel;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.ui.control.tree.FilterableTreeItem;

import java.util.function.Function;

/**
 * A filterable tree item of {@link ClassElement}.
 *
 * @author Matt Coley
 */
public class ClassItem extends FilterableTreeItem<ClassElement> {
	/**
	 * @param value
	 * 		Element to hold.
	 */
	public ClassItem(@Nonnull ClassElement value) {
		setValue(value);
	}

	/**
	 * Creates and adds a child item based on the provided child value.
	 *
	 * @param prefix
	 * 		Prefix of child value.
	 * @param element
	 * 		Child value.
	 * @param stringMapper
	 * 		Mapper from value to display string.
	 * @param graphicMapper
	 * 		Mapper from value to display graphic.
	 * @param menuMapper
	 * 		Mapper from value to context menu.
	 * @param <E>
	 * 		Child value type.
	 *
	 * @return Created child item.
	 */
	@Nonnull
	public <E> ClassItem item(@Nonnull String prefix, @Nonnull E element,
	                          @Nonnull Function<E, String> stringMapper,
	                          @Nonnull Function<E, Node> graphicMapper,
	                          @Nonnull Function<E, ContextMenu> menuMapper) {
		ClassItem item = new ClassItem(new LazyClassElement<>(prefix, element, stringMapper, graphicMapper, menuMapper));
		item(item);
		return item;
	}

	/**
	 * Adds a given child item.
	 *
	 * @param item
	 * 		Child item.
	 */
	public void item(@Nonnull ClassItem item) {
		getSourceChildren().add(item);
	}
}
