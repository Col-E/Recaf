package me.coley.recaf.mapping;

import me.coley.recaf.plugin.tools.Tool;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Mapping wrapper.
 *
 * @author Wolfie / win32kbase
 */
public class MappingsTool extends Tool<MappingsOption> {
	private final Supplier<Mappings> supplier;

	protected MappingsTool(Supplier<Mappings> supplier) {
		super(supplier.get().implementationName(), "1.0");
		this.supplier = supplier;
	}

	/**
	 * @return New mappings instance.
	 */
	public Mappings create() {
		return supplier.get();
	}

	/**
	 * @return {@code true} when the mappings implementation supports exporting through text.
	 */
	public boolean supportsTextExport() {
		return supplier.get().supportsExportText();
	}

	@Override
	protected Map<String, MappingsOption> createDefaultOptions() {
		return Collections.emptyMap();
	}
}
