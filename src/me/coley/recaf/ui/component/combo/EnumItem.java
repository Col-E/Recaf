package me.coley.recaf.ui.component.combo;

/**
 * Wrapper for Enum value with a set name for display.
 * 
 * @author Matt
 *
 * @param <V>
 */
public class EnumItem<V extends Enum<?>> {
	private final String display;
	final V enumValue;

	public EnumItem(String s, V value) {
		this.display = s;
		this.enumValue = value;
	}

	@Override
	public String toString() {
		return this.display;
	}

	public V getEnumValue() {
		return enumValue;
	}
}