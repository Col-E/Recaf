package me.coley.recaf.scripting;

import me.coley.recaf.util.Directories;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents an executable Beanshell script.
 */
public class Script {
    private static final String EXTENSION = ".bsh";

    private String name;
    private final String source;
    private final boolean isFile;
    private final Map<String, String> tags = new HashMap<>();

    // Matches:
    //      @key   value
    private static final Pattern TAG_PATTERN = Pattern.compile("//(\\s+)?@(?<key>\\S+)\\s+(?<value>.+)");

    // No public construction
    private Script(String source, boolean isFile) {
        this.source = source;
        this.isFile = isFile;

        if(isFile) {
            try {
                parseTags();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Instantiate a new script from a file.
     *
     * @param path
     *      The script file path
     * @return
     *      The script instance
     */
    public static Script fromPath(Path path) {
        return new Script(path.toString(), true);
    }

    /**
     * Instantiate a new script from raw source code.
     *
     * @param source
     *      The script source
     * @return
     *      The script instance
     */
    public static Script fromSource(String source) {
        return new Script(source, false);
    }

    /**
     * Scan the 'scripts' directory for scripts.
     *
     * @return
     *      A list of scrips or {@code null} if none could be found
     */
    public static List<Script> getAvailableScripts() {
        try(Stream<Path> stream = Files.walk(Directories.getScriptsDirectory(), FileVisitOption.FOLLOW_LINKS)) {
            return stream.filter(
                    f -> f.toString().endsWith(Script.EXTENSION)
            ).map(Script::fromPath).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parses the metadata tags.
     */
    private void parseTags() throws IOException {
        // Stream in the lines, so we don't have load the entire file into memory
        try(BufferedReader reader = Files.newBufferedReader(Path.of(source))) {
            String line;
            boolean started = false;
            while((line = reader.readLine()) != null) {
                if(!line.startsWith("//"))
                    continue;

                if(line.contains("==Metadata==")) {
                    started = true;
                    continue;
                }

                if(started && line.contains("==/Metadata=="))
                    return; // End of metadata, we're done

                if(!started)
                    continue;

                Matcher matcher = TAG_PATTERN.matcher(line);
                if(matcher.matches()) {
                    String key = matcher.group("key").toLowerCase();
                    String value = matcher.group("value");
                    tags.put(key, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Execute the script and return a result.
     *
     * @return
     *      The execution {@link ScriptResult result}.
     */
    public ScriptResult execute() {
        if(isFile)
            return ScriptEngine.execute(Path.of(source));

        return ScriptEngine.execute(source);
    }

    /**
     * @return The script's name from either metadata or filename
     */
    public String getName() {
        if(name == null) {
            String taggedName = getTag("name");

            if(taggedName != null)
                name = taggedName;
            else if(isFile)
                name = Path.of(source).getFileName().toString();
            else
                name = "Untitled script";
        }

        return name;
    }

    /**
     * @return The script's source as a path
     */
    public Path getPath() {
        return Path.of(source);
    }

    /**
     * Get a metadata tag by key.
     *
     * @param key
     *      The tag's key
     * @return
     *      The tag or {@code null} if it doesn't exist
     */
    public String getTag(String key) {
        return tags.getOrDefault(key, null);
    }
}
