package me.coley.recaf.mapping;

import jakarta.enterprise.context.ApplicationScoped;
import me.coley.recaf.mapping.format.*;
import me.coley.recaf.plugin.tools.ToolManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages mapping tool implementations.
 *
 * @author Matt Coley
 * @author Wolfie / win32kbase
 */
@ApplicationScoped
public class MappingsManager extends ToolManager<MappingsTool> {
	/**
	 * Registers all mapping tools.
	 */
	public MappingsManager() {
		register(new MappingsTool(SimpleMappings::new));
		register(new MappingsTool(EnigmaMappings::new));
		register(new MappingsTool(TinyV1Mappings::new));
		register(new MappingsTool(JadxMappings::new));
		register(new MappingsTool(SrgMappings::new));
		register(new MappingsTool(ProguardMappings::new));
	}
}
