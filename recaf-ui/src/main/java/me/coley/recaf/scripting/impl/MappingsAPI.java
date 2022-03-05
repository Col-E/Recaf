package me.coley.recaf.scripting.impl;

import me.coley.recaf.RecafUI;
import me.coley.recaf.mapping.*;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;

public class MappingsAPI {
    private static Logger logger = Logging.get(MappingsAPI.class);

    public static AggregatedMappings getAggregatedMappings() {
        MappingsManager mappingsManager = RecafUI.getController().getServices().getMappingsManager();
        return mappingsManager.getAggregatedMappings();
    }

    public static void applyMappings(MappingsTool mappingsTool, String mappingsText, Resource resource) {
        Mappings mappings = mappingsTool.create();
        mappings.parse(mappingsText);
        MappingUtils.applyMappings(ClassReader.EXPAND_FRAMES, 0, RecafUI.getController(), resource, mappings);
    }

    public static String exportMappings(MappingsTool mappingsTool) {
        Mappings currentAggregate = RecafUI.getController().getServices().getMappingsManager().getAggregatedMappings();

        if (!currentAggregate.supportsExportIntermediate()) {
            logger.error("Cannot export aggregated mappings, intermediate export not supported!");
            return null;
        }

        Mappings targetMappings = mappingsTool.create();
        targetMappings.importIntermediate(currentAggregate.exportIntermediate());

        return targetMappings.exportText();
    }
}
