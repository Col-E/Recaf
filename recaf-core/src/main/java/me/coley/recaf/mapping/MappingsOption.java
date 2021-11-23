package me.coley.recaf.mapping;

import me.coley.recaf.plugin.tools.ToolOption;

/**
 * Wrapper for mapping options, as required by the API.
 * These aren't ever actually used, so for now this is a dummy implementation.
 *
 * @author Wolfie / win32kbase
 */
public class MappingsOption extends ToolOption<Object> {
	/**
	 * Dummy option value.
	 */
	public MappingsOption() {
		super(null, null, null, null);
	}
}
