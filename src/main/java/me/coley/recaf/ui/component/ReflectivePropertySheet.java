package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.DefaultPropertyEditorFactory;
import org.controlsfx.property.editor.PropertyEditor;

import impl.org.controlsfx.skin.PropertySheetSkin;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import me.coley.recaf.config.Conf;
import me.coley.recaf.util.*;

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
	 *            Instances of classes to populate the property table.
	 */
	public ReflectivePropertySheet(Object... instances) {
		add0Hook();
		addCssHook();
		for (Object instance : instances)
			setupItems(instance);
	}

	/**
	 * Setup items of PropertySheet based on annotated fields in the given
	 * instance class.
	 *
	 * @param instance
	 *            Class containing fields, marked with annotations to denote
	 *            they should be added to the property sheet.
	 */
	protected void setupItems(Object instance) {
		for (Field field : Reflect.fields(instance.getClass())) {
			// Require conf annotation
			Conf conf = field.getDeclaredAnnotation(Conf.class);
			if (conf == null) continue;
			else if (conf.hide()) continue;
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
		 *         field}.
		 */
		public Object getOwner() {
			return owner;
		}

		/**
		 * @return Field this item represents.
		 */
		public Field getField() {
			return field;
		}

		/**
		 * @return Type of value with generic information included.
		 */
		public ParameterizedType getGenericType() {
			Type type = field.getGenericType();
			if (type instanceof ParameterizedType) {
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
			if (type == null) {
				// call default implmentation in Item.
				return Item.super.getPropertyEditorClass();
			}
			return JavaFX.optional(type);
		}

		/**
		 * @return {@code true} if caller is not initializing.
		 */
		protected boolean checkCaller() {
			StackTraceElement[] trace = Thread.currentThread().getStackTrace();
			for (int i = 5; i < trace.length; i++) {
				StackTraceElement item = trace[i];
				if (item.getMethodName().contains("<")) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * Custom editor for a reflective item for non-standard property types.
	 *
	 * @param <T>
	 *            Type of value being modified.
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

	/**
	 * Flag for callback being set.
	 */
	private boolean hooked;

	/**
	 * Add hook that allows us to set values to 0... I'm not joking. Comment out
	 * this method and try to set a value to 0. I have no idea why the hell this
	 * would be a problem but it is. I tried debugging it for about 4 hours
	 * today and gave up. This is for now, the "solution".
	 */
	protected void add0Hook() {
		if (!hooked) {
			SimpleObjectProperty<Callback<PropertySheet.Item, PropertyEditor<?>>> propertyEditorFactory = new SimpleObjectProperty<>(
					this, "propertyEditor", new DefaultPropertyEditorFactory());
			setPropertyEditorFactory(getItemPropertyEditorCallback(propertyEditorFactory));
			hooked = true;
		}
	}
	
	/**
	 * Hack that makes the property-sheet scrollpane fit the parent height. This
	 * makes styling it less of a pain in the ass.
	 */
	protected void addCssHook() {
		// Can't be done initially.
		// has to be done after the scene has been constructed.
		Threads.runLaterFx(30, () -> {
			try {
				PropertySheetSkin skin = (PropertySheetSkin) getSkin();
				ScrollPane s = Reflect.get(skin, "scroller");
				s.fitToHeightProperty().setValue(true);
				// Force scrollpane to update, fill the height as specified by
				// the newly set property. This will involve a short flicker but
				// oh well.
				s.getParent().getStyleClass().add("recalc");
			} catch (Exception e) {
				// Can safely ignore. This really only affects themes that
				// actively change the background color of this component.
			}
		});
	}

	@SuppressWarnings("unchecked")
	private Callback<PropertySheet.Item, PropertyEditor<?>> getItemPropertyEditorCallback(
			SimpleObjectProperty<Callback<PropertySheet.Item, PropertyEditor<?>>> propertyEditorFactory) {
		return param -> {
			PropertyEditor<Object> editor = (PropertyEditor<Object>) propertyEditorFactory.get().call(param);
			if (editor != null && editor.getEditor() instanceof TextField) editor.getEditor().setOnKeyReleased(new EventHandler<KeyEvent>() {
				public void handle(KeyEvent ke) {
					Class<?> type = param.getType();
					if (Number.class.isAssignableFrom(type) || (type.equals(int.class) || type.equals(long.class) || type.equals(
							float.class) || type.equals(double.class))) {
						String s = ((TextField) editor.getEditor()).getText();
						try {
							Object val = null;
							if (type.equals(int.class) || type.equals(Integer.class)) {
								val = Integer.parseInt(s);
							} else if (type.equals(long.class) || type.equals(Long.class)) {
								val = Long.parseLong(s);
							} else if (type.equals(double.class) || type.equals(Double.class)) {
								val = Double.parseDouble(s);
							} else if (type.equals(float.class) || type.equals(Float.class)) {
								val = Float.parseFloat(s);
							}
							// Yes, this is all just to set a value to 0...
							if (val != null && val.equals(0)) {
								param.setValue(val);
							}
						} catch (Exception e) {}
						// disregard number format exceptions
					}

				}
			});
			return editor;
		};
	}
}