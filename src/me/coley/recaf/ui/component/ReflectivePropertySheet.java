package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.PropertyEditor;
import javafx.beans.value.ObservableValue;
import me.coley.recaf.config.Conf;
import me.coley.recaf.util.JavaFX;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.Reflect;

/**
 * Reflection powered PropertySheet. Loads values from an instance. Fields
 * should be marked with a {@link me.coley.recaf.config.Conf} annotation to be
 * added to the sheet.
 *
 * @author Matt
 */
public class ReflectivePropertySheet extends PropertySheet {
	/**
	 * Create a PropertySheet by parsing fields of a class.
	 *
	 * @param instances
	 * 		Instances of classes to populate the property table.
	 */
	public ReflectivePropertySheet(Object... instances) {
		for(Object instance : instances)
			setupItems(instance);
	}

	/**
	 * Setup items of PropertySheet based on annotated fields in the given instance class.
	 *
	 * @param instance
	 * 		Class containing fields, marked with annotations to denote they should be added to the property sheet.
	 */
	protected void setupItems(Object instance) {
		for(Field field : Reflect.fields(instance.getClass())) {
			// Require conf annotation
			Conf conf = field.getDeclaredAnnotation(Conf.class);
			if(conf == null)
				continue;
			// Setup item & add to list
			getItems().add(new ReflectiveItem(instance, field, conf.category(), conf.key()));
		}
	}

	/**
	 * Reflection-powered PropertySheet item.
	 *
	 * @author Matt
	 */
	public static class ReflectiveItem implements Item {
		private final String categoryKey, translationKey;
		private final Field field;
		private final Supplier<Object> getter;
		private final Consumer<Object> setter;
		private final Object owner;

		public ReflectiveItem(Object owner, Field field, String categoryKey, String translationKey) {
			this.categoryKey = categoryKey;
			this.translationKey = translationKey;
			this.owner = owner;
			this.field = field;
			getter = () -> Reflect.get(owner, field);
			setter = (value) -> Reflect.set(owner, field, value);
		}

		/**
		 * @return Object instance of class that contains the {@link #getField()
		 * field}.
		 */
		protected Object getOwner() {
			return owner;
		}

		/**
		 * @return Field this item represents.
		 */
		protected Field getField() {
			return field;
		}

		/**
		 * @return Type of value with generic information included.
		 */
		protected ParameterizedType getGenericType() {
			Type type = field.getGenericType();
			if(type instanceof ParameterizedType) {
				return (ParameterizedType) type;
			}
			return null;
		}

		@Override
		public Class<?> getType() {
			return field.getType();
		}

		@Override
		public String getCategory() {
			return Lang.get(categoryKey);
		}

		@Override
		public String getName() {
			return Lang.get(categoryKey + "." + translationKey + ".name");
		}

		@Override
		public String getDescription() {
			return Lang.get(categoryKey + "." + translationKey + ".desc");
		}

		@Override
		public Object getValue() {
			return getter.get();
		}

		@Override
		public void setValue(Object value) {
			setter.accept(value);
		}

		@Override
		public Optional<ObservableValue<? extends Object>> getObservableValue() {
			// It would be proper to have this be a field getter, and change
			// get/setter methods but it works as-is.
			return JavaFX.optionalObserved(getValue());
		}

		/**
		 * @return Type of editor for the represented value of this item.
		 */
		protected Class<?> getEditorType() {
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public final Optional<Class<? extends PropertyEditor<?>>> getPropertyEditorClass() {
			// Check if there is a custom editor for this item.
			Class<? extends PropertyEditor<?>> type = (Class<? extends CustomEditor<?>>) getEditorType();
			if(type == null) {
				// call default implmentation in Item.
				return Item.super.getPropertyEditorClass();
			}
			return JavaFX.optional(type);
		}
	}

	/**
	 * Custom editor for a reflective item for non-standard property types.
	 *
	 * @param <T>
	 * 		Type of value being modified.
	 *
	 * @author Matt
	 */
	public static abstract class CustomEditor<T> implements PropertyEditor<T> {
		protected final ReflectiveItem item;

		public CustomEditor(Item item) {
			this.item = (ReflectiveItem) item;
		}

		@SuppressWarnings("unchecked")
		@Override
		public T getValue() {
			return (T) item.getValue();
		}

		@Override
		public void setValue(T value) {
			item.setValue(value);
		}
	}
}