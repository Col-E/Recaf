package me.coley.recaf.ui.component.combo;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.function.Consumer;

import javax.swing.JComboBox;

/**
 * ComboBox populated with Enum.values().
 * 
 * @author Matt
 *
 * @param <T>
 */
@SuppressWarnings("serial")
public class EnumCombobox<T extends Enum<?>> extends JComboBox<EnumItem<T>> {
	private final Consumer<T> consumer;

	public EnumCombobox(T[] values) {
		this(values, null);
	}

	public EnumCombobox(T[] values, Consumer<T> consumer) {
		this.consumer = consumer;
		// Populate combo values and set intitial selection
		for (T value : values) {
			add(getText(value), value);
		}
		setSelected(values[0]);
		addListener();
	}

	/**
	 * Called when the value is updated.
	 * 
	 * @param value
	 */
	public void onEnumSelection(T value) {
		if (consumer != null) {
			consumer.accept(value);
		}
	}

	/**
	 * @param value
	 *            Enum value.
	 * @return Display text of value.
	 */
	protected String getText(T value) {
		return value.name();
	}

	/**
	 * Add an item to the model.
	 * 
	 * @param name
	 *            Display for value.
	 * @param value
	 *            Held value.
	 */
	public final void add(String name, T value) {
		addItem(new EnumItem<T>(name, value));
	}

	/**
	 * Set the selected value.
	 * 
	 * @param value
	 */
	public final void setSelected(T value) {
		for (int i = 0; i < getItemCount(); i++) {
			EnumItem<T> item = getItemAt(i);
			if (value == item.getEnumValue()) {
				setSelectedIndex(i);
				return;
			}
		}
	}

	/**
	 * Add a listener to call {@link #onEnumSelection(Enum)}.
	 */
	private final void addListener() {
		addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				onEnumSelection(getEnumSelection());
			}
		});
	}

	/**
	 * @return Type of selection.
	 */
	@SuppressWarnings("unchecked")
	public final T getEnumSelection() {
		Object item = this.getSelectedItem();
		if (item == null) return null;
		return (T) ((EnumItem<?>) item).getEnumValue();
	}
}