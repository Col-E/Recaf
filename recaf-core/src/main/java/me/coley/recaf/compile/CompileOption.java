package me.coley.recaf.compile;

import me.coley.recaf.plugin.tools.ToolOption;

/**
 * Wrapper for compiler options so each compiler option set is not handled specifically based on implementation.
 *
 * @param <T>
 * 		Option type. Exposed as {@link #getValueType()}.
 *
 * @author Matt Coley
 */
public class CompileOption<T> extends ToolOption<T> {
	/**
	 * @param valueType
	 * 		Type of supported values.
	 * @param optionName
	 * 		Option name as defined by the compiler.
	 * @param description
	 * 		Description of what the option controls in the compiler output.
	 * @param defaultValue
	 * 		Default value for the option.
	 */
	public CompileOption(Class<T> valueType, String optionName, String description, T defaultValue) {
		this(valueType, optionName, optionName, description, defaultValue);
	}

	/**
	 * @param valueType
	 * 		Type of supported values.
	 * @param optionName
	 * 		Option name as defined by the compiler.
	 * @param recafNameAlias
	 * 		Alias for the option name, used by Recaf to consolidate the
	 * 		<i>"same"</i> feature across different compiler implementations.
	 * @param description
	 * 		Description of what the option controls in the compiler output.
	 * @param defaultValue
	 * 		Default value for the option.
	 */
	public CompileOption(Class<T> valueType, String optionName, String recafNameAlias, String description,
						   T defaultValue) {
		super(valueType, optionName, recafNameAlias, description, defaultValue);
	}
}
