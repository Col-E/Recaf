package me.coley.recaf.analysis;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jregex.MatchIterator;
import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.ClassSourceType;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.util.RegexUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.visitor.TypeCollectionClassVisitor;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resources;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Attempts to find classes that are referenced by code that will execute at runtime.
 *
 * @author yapht
 */
public class ClassReferenceAnalyzer {
    private static final Logger logger = Logging.get(ClassReferenceAnalyzer.class);
    private static final Gson GSON = new Gson();
    private static final Pattern TYPE_PATTERN = RegexUtil.pattern("(\\[+)?L?([^;<>\\(\\)]+)(;\\)>?[ZV])?");
    private final Resources resources;
    private final Set<CommonClassInfo> referencedClasses = new HashSet<>();
    private final Queue<ClassInfo> analysisQueue = new ArrayDeque<>();
    private CompletableFuture<Void> analyzeTask;

    public ClassReferenceAnalyzer(Resources resources) {
        this.resources = resources;

        ClassInfo entryClass = tryFindEntryPoint();
        if (entryClass != null) {
            analysisQueue.add(entryClass);
            analyzeTask = CompletableFuture.runAsync(this::run);
            logger.info("Scanning for referenced classes start from: {}", entryClass.getName());
        } else logger.error("Failed to find suitable entry class");
    }

    public ClassReferenceAnalyzer(Workspace workspace) {
        this(workspace.getResources());
    }

    private void addClasses(String descriptor) {
        MatchIterator it = TYPE_PATTERN.matcher(descriptor).findAll();
        if (!it.hasMore()) return;
        while (it.hasMore()) {
            String name = it.nextMatch().group(2);
            if (name == null) continue;
            ClassInfo info = resources.getClass(name);
            if (info == null) continue;
            // Ignore internal libraries
            if (info.getSourceType() == ClassSourceType.INTERNAL_LIBRARY) continue;
            if (referencedClasses.contains(info)) continue;
            referencedClasses.add(info);
            if (analysisQueue.contains(info)) continue;
            analysisQueue.add(info);
        }
    }

    private void run() {
        Set<String> typeDescriptors = new HashSet<>();

        while (!analysisQueue.isEmpty()) {
            ClassInfo info = analysisQueue.poll();
            ClassReader reader = info.getClassReader();
            reader.accept(new TypeCollectionClassVisitor(typeDescriptors, true), ClassReader.SKIP_DEBUG);

            for (String typeDesc : typeDescriptors) {
                if (typeDesc == null)
                    continue;
                addClasses(typeDesc);
            }

            typeDescriptors.clear();
        }

        logger.info("Found {} referenced classes", referencedClasses.size());
    }

    private JsonObject getFabricModInfo() {
        FileInfo fabricModJson = resources.getFile("fabric.mod.json");
        if (fabricModJson == null)
            return null;
        JsonObject object = GSON.fromJson(new String(fabricModJson.getValue()), JsonObject.class);
        // https://fabricmc.net/wiki/documentation:fabric_mod_json
        if (!object.has("schemaVersion") || object.get("schemaVersion").getAsInt() != 1)
            return null;
        return object;
    }

    private ClassInfo getFabricModEntry(JsonObject fabricInfo) {
        if (fabricInfo == null)
            return null;

        JsonObject entrypoints = fabricInfo.get("entrypoints").getAsJsonObject();

        String entryPath = null;
        for (String name : new String[]{"main", "client", "server"}) {
            if (entrypoints.has(name)) {
                JsonArray entries = entrypoints.get(name).getAsJsonArray();
                if (entries.isEmpty())
                    continue;

                entryPath = entries.get(0).getAsString();
                break;
            }
        }

        if (entryPath == null)
            return null;

        return resources.getClass(entryPath.replace('.', '/'));
    }

    private void referenceFabricMixins(JsonObject fabricInfo) {
        if (fabricInfo == null)
            return;

        if (!fabricInfo.has("mixins"))
            return;

        for (JsonElement mixinJsonPath : fabricInfo.get("mixins").getAsJsonArray()) {
            FileInfo mixinJsonFile = resources.getFile(mixinJsonPath.getAsString());
            if (mixinJsonFile == null)
                continue;

            JsonObject mixinsObject = GSON.fromJson(new String(mixinJsonFile.getValue()), JsonObject.class);
            if (mixinsObject == null || !mixinsObject.has("package"))
                return;

            String packagePath = mixinsObject.get("package").getAsString().replace('.', '/');
            if (!packagePath.endsWith("/"))
                packagePath += "/";

            Set<String> mixinClasses = new HashSet<>();

            for (String name : new String[]{"mixins", "client", "server"}) {
                if (mixinsObject.has(name)) {
                    JsonArray array = mixinsObject.get(name).getAsJsonArray();
                    for (JsonElement mixinClass : array) {
                        mixinClasses.add(mixinClass.getAsString());
                    }
                }
            }

            for (String mixinClass : mixinClasses) {
                String fullPath = packagePath + mixinClass.replace('.', '/');
                addClasses(fullPath);
            }

            logger.info("Referenced {} mixin classes from {}", mixinClasses.size(), mixinJsonFile.getName());
        }
    }

    private ClassInfo tryFindEntryPoint() {
        FileInfo manifestInfo = resources.getFile("META-INF/MANIFEST.MF");
        if (manifestInfo == null)
            return null;

        String data = new String(manifestInfo.getValue());
        // Parse Fabric related files
        if (data.contains("Fabric-")) {
            try {
                JsonObject modInfo = getFabricModInfo();
                ClassInfo modEntry = getFabricModEntry(modInfo);

                // Don't bother referencing any mixin classes if we couldn't find a valid entry class
                if (modEntry != null) {
                    referenceFabricMixins(modInfo);
                    return modEntry;
                }
            } catch (Exception e) {
                logger.error("Failed to parse Fabric data");
            }
        }

        // Find any suitable main(String[]) method, just in case
        ClassInfo fallbackEntryClass =
                resources.getClasses()
                        .filter(info -> info.findMethod("main", "([Ljava/lang/String;)V") != null)
                        .filter(method -> (method.getAccess() & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)) != 0)
                        .findFirst()
                        .orElse(null);

        Matcher matcher = RegexUtil.getMatcher("Main-Class: (.+)", data);
        if (!matcher.find())
            return fallbackEntryClass;

        String mainClassPath = matcher.group(1).replace('.', '/');

        ClassInfo entryClass = resources.getClass(mainClassPath);
        if (entryClass != null)
            return entryClass;

        return fallbackEntryClass;
    }

    /**
     * @param classInfo
     *          The class to check for
     * @return
     *          Whether the class name is referenced by code executed at runtime
     */
    public boolean isClassReferenced(CommonClassInfo classInfo) {
        if (analyzeTask == null)
            return true;

        // Analysis only takes a couple seconds so this should be fine
        if (!analyzeTask.isDone())
            analyzeTask.join();

        return referencedClasses.contains(classInfo);
    }
}