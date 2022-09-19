package me.coley.recaf.scripting.impl;

import me.coley.recaf.RecafUI;
import me.coley.recaf.mapping.AggregatedMappings;
import me.coley.recaf.mapping.MappingUtils;
import me.coley.recaf.mapping.Mappings;
import me.coley.recaf.mapping.MappingsManager;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;

/**
 * Utility functions for working with mappings.
 *
 * @author Wolfie / win32kbase
 */
public class MappingsAPI {
	private static final Logger logger = Logging.get(MappingsAPI.class);

	/**
	 * @return Current aggregate mappings of the current workspace.
	 * This contains a flattened history of all the user's applied mappings.
	 */
	public static AggregatedMappings getAggregatedMappings() {
		return getMappingsManager().getAggregatedMappings();
	}

	/**
	 * @param mappings
	 * 		Mappings implementation to export mappings into.
	 *
	 * @return The {@link #getAggregatedMappings()} in the given mappings format as text.
	 */
	public static String exportAggregateMappings(Mappings mappings) {
		Mappings currentAggregate = getMappingsManager().getAggregatedMappings();
		if (!currentAggregate.supportsExportIntermediate()) {
			logger.error("Cannot export aggregated mappings, intermediate export not supported!");
			return null;
		}

		mappings.importIntermediate(currentAggregate.exportIntermediate());
		return mappings.exportText();
	}

	/**
	 * @param mappings
	 * 		Mappings implementation. Will parse the given mappings text.
	 * @param mappingsText
	 * 		Text of the mappings.
	 * @param resource
	 * 		Resource to apply mappings to.
	 */
	public static void applyMappings(Mappings mappings, String mappingsText, Resource resource) {
		mappings.parse(mappingsText);
		MappingUtils.applyMappings(0, 0, RecafUI.getController(), resource, mappings);
	}

	/**
	 * @return The mappings manager.
	 */
	private static MappingsManager getMappingsManager() {
		return RecafUI.getController().getServices().getMappingsManager();
	}
}
