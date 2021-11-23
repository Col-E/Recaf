package me.coley.recaf.mapping;

import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

public class SimpleMappings extends MappingsAdapter {
    private final Logger logger = Logging.get(TinyMappings.class);

    public SimpleMappings() {
        super("Simple", true, true);
    }

    @Override
    public void parse(String mappingText) {
        String[] lines = mappingText.split("[\n\r]");

        for (String line : lines) {
            // Skip comments and empty lines
            if (line.trim().startsWith("#") || line.trim().isEmpty())
                continue;

            String[] args = line.split(" ");
            String oldName = EscapeUtil.unescape(args[0]);
            if (args.length > 2) {
                String oldDesc = EscapeUtil.unescape(args[1]);
                String newName = EscapeUtil.unescape(args[2]);
                addField(oldName, oldDesc, newName);
            } else {
                String newName = EscapeUtil.unescape(args[1]);
                addClass(oldName, newName);
            }
        }
    }
}
