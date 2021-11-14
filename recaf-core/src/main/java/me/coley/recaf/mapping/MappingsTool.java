package me.coley.recaf.mapping;

import me.coley.recaf.plugin.tools.Tool;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

public class MappingsTool extends Tool<MappingsOption> {
    private final Supplier<Mappings> supplier;

    protected MappingsTool(Supplier<Mappings> supplier) {
        super(supplier.get().implementationName(), "1.0");
        this.supplier = supplier;
    }

    public Mappings parse(String mappingsText) {
        Mappings mappings = supplier.get();
        mappings.parse(mappingsText);
        return mappings;
    }

    @Override
    protected Map<String, MappingsOption> createDefaultOptions() {
        return Collections.emptyMap();
    }
}
