package software.coley.recaf.ui.pane.editing.jvm.lowlevel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;

import java.util.function.Function;

/**
 * An implementation of {@link ClassElement} that lazily computes values based on a given value.
 *
 * @param <E>
 * 		Element value type.
 *
 * @author Matt Coley
 */
public class LazyClassElement<E> implements ClassElement {
	private final String prefix;
	private final E element;
	private final Function<E, String> stringMapper;
	private final Function<E, Node> graphicMapper;
	private final Function<E, ContextMenu> menuMapper;

	public LazyClassElement(@Nonnull String prefix, @Nonnull E element,
	                        @Nonnull Function<E, String> stringMapper,
	                        @Nonnull Function<E, Node> graphicMapper,
	                        @Nonnull Function<E, ContextMenu> menuMapper) {
		this.prefix = prefix;
		this.element = element;
		this.stringMapper = stringMapper;
		this.graphicMapper = graphicMapper;
		this.menuMapper = menuMapper;
	}

	@Nonnull
	public E getElement() {
		return element;
	}

	@Nonnull
	@Override
	public String prefix() {
		return prefix;
	}

	@Nonnull
	@Override
	public String content() {
		return stringMapper.apply(element);
	}

	@Nullable
	@Override
	public Node graphic() {
		return graphicMapper.apply(element);
	}

	@Nullable
	@Override
	public EventHandler<ContextMenuEvent> contextRequest() {
		return e -> menuMapper.apply(element);
	}
}
