package me.coley.recaf.decompile;

/**
 * Wrapper for decompiler options so each decompiler option set is not handled specifically based on implementation.
 *
 * @param <T>
 * 		Option type. Exposed as {@link #getValueType()}.
 *
 * @author Matt Coley
 */
public class DecompileOption<T> {
	private final Class<T> valueType;
	private final String optionName;
	private final String recafNameAlias;
	private final String description;
	private final T defaultValue;

	/**
	 * @param valueType
	 * 		Type of supported values.
	 * @param optionName
	 * 		Option name as defined by the decompiler.
	 * @param description
	 * 		Description of what the option controls in the decompiler output.
	 * @param defaultValue
	 * 		Default value for the option.
	 */
	public DecompileOption(Class<T> valueType, String optionName, String description, T defaultValue) {
		this(valueType, optionName, optionName, description, defaultValue);
	}

	/**
	 * @param valueType
	 * 		Type of supported values.
	 * @param optionName
	 * 		Option name as defined by the decompiler.
	 * @param recafNameAlias
	 * 		Alias for the option name, used by Recaf to consolidate the <i>"same"</i> feature across different decompiler implementations.
	 * @param description
	 * 		Description of what the option controls in the decompiler output.
	 * @param defaultValue
	 * 		Default value for the option.
	 */
	public DecompileOption(Class<T> valueType, String optionName, String recafNameAlias, String description, T defaultValue) {
		this.valueType = valueType;
		this.optionName = optionName;
		this.recafNameAlias = recafNameAlias;
		this.description = description;
		this.defaultValue = defaultValue;
	}

	/**
	 * @return Type of supported values.
	 */
	public Class<T> getValueType() {
		return valueType;
	}

	/**
	 * @return Option name as defined by the decompiler.
	 */
	public String getOptionName() {
		return optionName;
	}

	/**
	 * @return Alias for the option name, used by Recaf to consolidate the <i>"same"</i> feature across different decompiler implementations.
	 */
	public String getRecafNameAlias() {
		return recafNameAlias;
	}

	/**
	 * @return Description of what the option controls in the decompiler output.
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
}
