package me.coley.recaf.plugin.tools;

import java.util.Objects;

/**
 * Wrapper for tool options so each tool option set is not handled specifically based on implementation.
 *
 * @param <T>
 * 		Option type. Exposed as {@link #getValueType()}.
 *
 * @author Matt Coley
 */
public class ToolOption<T> {
	private final Class<T> valueType;
	private final String optionName;
	private final String recafNameAlias;
	private final String description;
	private final T defaultValue;
	private T value;

	/**
	 * @param valueType
	 * 		Type of supported values.
	 * @param optionName
	 * 		Option name as defined by the tool.
	 * @param description
	 * 		Description of what the option controls in the tool output.
	 * @param defaultValue
	 * 		Default value for the option.
	 */
	public ToolOption(Class<T> valueType, String optionName, String description, T defaultValue) {
		this(valueType, optionName, optionName, description, defaultValue);
	}

	/**
	 * @param valueType
	 * 		Type of supported values.
	 * @param optionName
	 * 		Option name as defined by the tool.
	 * @param recafNameAlias
	 * 		Alias for the option name, used by Recaf to consolidate the
	 * 		<i>"same"</i> feature across different tool implementations.
	 * @param description
	 * 		Description of what the option controls in the tool output.
	 * @param defaultValue
	 * 		Default value for the option.
	 */
	public ToolOption(Class<T> valueType, String optionName, String recafNameAlias, String description,
						 T defaultValue) {
		this.valueType = valueType;
		this.optionName = optionName;
		this.recafNameAlias = recafNameAlias;
		this.description = description;
		this.defaultValue = defaultValue;
		setValue(defaultValue);
	}

	/**
	 * @return Type of supported values.
	 */
	public Class<T> getValueType() {
		return valueType;
	}

	/**
	 * @return Option name as defined by the tool.
	 */
	public String getOptionName() {
		return optionName;
	}

	/**
	 * @return Alias for the option name, used by Recaf to consolidate the
	 * <i>"same"</i> feature across different tool implementations.
	 */
	public String getRecafNameAlias() {
		return recafNameAlias;
	}

	/**
	 * @return Description of what the option controls in the tool output.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return Default value for the option.
	 */
	public T getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @return Current value for the option.
	 */
	public T getValue() {
		return value;
	}

	/**
	 * @param value
	 * 		New value for the option.
	 */
	public void setValue(T value) {
		this.value = Objects.requireNonNull(value, "Option value[" + getRecafNameAlias() + "] cannot be null!");
	}
}
