package software.coley.recaf.ui.pane.editing.jvm.lowlevel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Registry for mapping class-file model values into {@link ClassItem} nodes.
 *
 * @author Matt Coley
 */
public final class ClassItemRegistry {
	private final Map<Class<?>, Definition<?>> definitions = new LinkedHashMap<>();

	ClassItemRegistry() {
		register(String.class);
		register(Number.class);
		register(Boolean.class);
		register(Character.class);
		register(NullValue.class).text(value -> "<null>");
	}

	/**
	 * Registers a type with the default display behavior.
	 *
	 * @param type
	 * 		Type to register.
	 *
	 * @return Mutable definition for the registered type.
	 *
	 * @throws IllegalStateException
	 * 		If the type is already registered.
	 */
	@Nonnull
	public <E> Definition<E> register(@Nonnull Class<E> type) {
		if (definitions.containsKey(type))
			throw new IllegalStateException("Type already registered: " + type.getName());
		Definition<E> definition = new Definition<>();
		definitions.put(type, definition);
		return definition;
	}

	/**
	 * Creates the hidden-root item for a class-file tree.
	 *
	 * @param prefix
	 * 		Root prefix.
	 * @param value
	 * 		Root value.
	 *
	 * @return Created root item.
	 *
	 * @throws IllegalStateException
	 * 		If no definition matches the value.
	 */
	@Nonnull
	public <E> ClassItem createRoot(@Nonnull String prefix, @Nonnull E value) {
		return create(null, prefix, value, definitionFor(value));
	}

	/**
	 * Adds a registered value below a parent item.
	 *
	 * @param parent
	 * 		Parent item.
	 * @param prefix
	 * 		Child prefix.
	 * @param value
	 * 		Child value.
	 *
	 * @return Created child item.
	 *
	 * @throws IllegalStateException
	 * 		When no definition matches the value.
	 */
	@Nonnull
	public <E> ClassItem add(@Nonnull ClassItem parent, @Nonnull String prefix, @Nonnull E value) {
		return create(parent, prefix, value, definitionFor(value));
	}

	/**
	 * Adds a value with an ad-hoc definition without changing the registry.
	 *
	 * @param parent
	 * 		Parent item.
	 * @param prefix
	 * 		Child prefix.
	 * @param value
	 * 		Child value.
	 * @param configure
	 * 		Definition customization.
	 *
	 * @return Created child item.
	 */
	@Nonnull
	public <E> ClassItem add(@Nonnull ClassItem parent, @Nonnull String prefix, @Nonnull E value,
	                         @Nonnull Consumer<Definition<E>> configure) {
		Definition<E> definition = new Definition<>();
		configure.accept(definition);
		return create(parent, prefix, value, definition);
	}

	/**
	 * Adds a nullable value, making an absent value explicit in the tree.
	 *
	 * @param parent
	 * 		Parent item.
	 * @param prefix
	 * 		Child prefix.
	 * @param value
	 * 		Nullable child value.
	 *
	 * @return Created child item.
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	public <E> ClassItem addNullable(@Nonnull ClassItem parent, @Nonnull String prefix, @Nullable E value) {
		if (value == null)
			return add(parent, prefix, (E) NullValue.INSTANCE);
		return add(parent, prefix, value);
	}

	@Nonnull
	private <E> ClassItem create(@Nullable ClassItem parent, @Nonnull String prefix, @Nonnull E value,
	                             @Nonnull Definition<E> definition) {
		ClassItem item = new ClassItem(new LazyClassElement<>(prefix, value,
				definition.textMapper, definition.graphicMapper, definition.contextMenuMapper));
		if (parent == null)
			return populate(item, value, definition);
		parent.item(item);
		return populate(item, value, definition);
	}

	@Nonnull
	private <E> ClassItem populate(@Nonnull ClassItem item, @Nonnull E value, @Nonnull Definition<E> definition) {
		for (BiConsumer<ClassItem, E> populator : definition.childPopulators)
			populator.accept(item, value);
		return item;
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	private <E> Definition<E> definitionFor(@Nonnull E value) {
		Class<?> type = value.getClass();
		Definition<?> exact = definitions.get(type);
		if (exact != null)
			return (Definition<E>) exact;

		Class<?> superclass = type.getSuperclass();
		while (superclass != null) {
			Definition<?> definition = definitions.get(superclass);
			if (definition != null)
				return (Definition<E>) definition;
			superclass = superclass.getSuperclass();
		}
		throw new IllegalStateException("No class item definition for: " + type.getName());
	}

	/**
	 * Singleton instance for null values.
	 */
	private static final class NullValue {
		private static final NullValue INSTANCE = new NullValue();
	}

	/**
	 * Mutable display and child-population definition for one registered type.
	 *
	 * @param <E>
	 * 		Registered value type.
	 */
	public static final class Definition<E> {
		private Function<E, String> textMapper = String::valueOf;
		private Function<E, Node> graphicMapper = value -> null;
		private Function<E, ContextMenu> contextMenuMapper = value -> null;
		private final List<BiConsumer<ClassItem, E>> childPopulators = new ArrayList<>();

		/**
		 * Replaces the textual display mapper.
		 *
		 * @param mapper
		 * 		Text mapper.
		 *
		 * @return This definition.
		 */
		@Nonnull
		public Definition<E> text(@Nonnull Function<E, String> mapper) {
			textMapper = mapper;
			return this;
		}

		/**
		 * Replaces the graphic mapper.
		 *
		 * @param mapper
		 * 		Graphic mapper.
		 *
		 * @return This definition.
		 */
		@Nonnull
		public Definition<E> graphic(@Nonnull Function<E, Node> mapper) {
			graphicMapper = mapper;
			return this;
		}

		/**
		 * Replaces the context-menu mapper.
		 *
		 * @param mapper
		 * 		Context-menu mapper.
		 *
		 * @return This definition.
		 */
		@Nonnull
		public Definition<E> contextMenu(@Nonnull Function<E, ContextMenu> mapper) {
			contextMenuMapper = mapper;
			return this;
		}

		/**
		 * Appends a child populator in registration order.
		 *
		 * @param populator
		 * 		Child populator.
		 *
		 * @return This definition.
		 */
		@Nonnull
		public Definition<E> children(@Nonnull BiConsumer<ClassItem, E> populator) {
			childPopulators.add(populator);
			return this;
		}
	}
}
