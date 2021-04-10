package me.coley.recaf.mapping;

import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.Workspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * TSRG mappings file implementation.
 *
 * @author Filip (1fxe)
 */
public class TSrgMappings extends FileMappings {
    private static final String FAIL = "Invalid TSrg mappings, ";

    /**
     * Constructs mappings from a given file.
     *
     * @param path      A path to a file containing TSrg styled mappings.
     * @param workspace Workspace to pull names from when using hierarchy lookups.
     * @throws IOException Thrown if the file could not be read.
     */
    TSrgMappings(Path path, Workspace workspace) throws IOException {
        super(path, workspace);
    }

    @Override
    protected Map<String, String> parse(String text) {
        String[] lines = StringUtil.splitNewline(text);
        Map<String, String> map = new HashMap<>(lines.length);
        int line = 0;
        String obfOwner = null;
        for (String lineStr : lines) {
            line++;
            String[] args = lineStr.trim().split(" ");
            try {
                // Fields and Methods start with a tab
                // class/Name new/Name
                //      fieldName newFieldName
                //      methodName methodDesc newMethodName
                if (!lineStr.startsWith("\t")) {
                    obfOwner = args[0];
                    String renamedClass = args[1];
                    map.put(obfOwner, renamedClass);
                } else {
                    if (args.length == 2) { // Field
                        String obfName = args[0];
                        String renamedName = args[1];
                        map.put(obfOwner + "." + obfName, renamedName);
                    } else if (args.length == 3) { // Method
                        String obfName = args[0];
                        String obfDesc = args[1];
                        String renamedName = args[2];
                        map.put(obfOwner + "." + obfName + obfDesc, renamedName);
                    }
                }
            } catch (IndexOutOfBoundsException ex) {
                throw new IllegalArgumentException(FAIL + "failed parsing line " + line, ex);
            }
        }
        return map;
    }
}
