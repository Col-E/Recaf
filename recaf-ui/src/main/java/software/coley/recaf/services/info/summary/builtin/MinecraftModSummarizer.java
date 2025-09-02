package software.coley.recaf.services.info.summary.builtin;

import atlantafx.base.theme.Styles;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.Batch;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

/**
 * Summarizer that finds Minecraft mods main mod classes.
 *
 * @Author Canrad
 */
@ApplicationScoped
public class MinecraftModSummarizer implements ResourceSummarizer {
    private final TextProviderService textService;
    private final IconProviderService iconService;
    private final Actions actions;

    @Inject
    public MinecraftModSummarizer(@Nonnull TextProviderService textService,
                                  @Nonnull IconProviderService iconService,
                                  @Nonnull Actions actions) {
        this.textService = textService;
        this.iconService = iconService;
        this.actions = actions;
    }

    @Override
    public boolean summarize(@Nonnull Workspace workspace,
                             @Nonnull WorkspaceResource resource,
                             @Nonnull SummaryConsumer consumer) {
        Batch batch = FxThreadUtil.batch();
        boolean foundAny = false;

        // Add title
        batch.add(() -> {
            Label title = new BoundLabel(Lang.getBinding("service.analysis.minecraft-mod-info"));
            title.getStyleClass().addAll(Styles.TITLE_4);
            consumer.appendSummary(title);
        });

        // 1. Try to find Fabric mod information
        FileInfo fabricFileInfo = resource.getFileBundle().get("fabric.mod.json");
        if (fabricFileInfo != null) {
            foundAny = true;
            String mcVersion = "";
            List<String> mainClasses = new ArrayList<>();

            try {
                String jsonText = fabricFileInfo.asTextFile().getText();
                JsonObject json = JsonParser.parseString(jsonText).getAsJsonObject();
                if (json.has("entrypoints")) {
                    JsonObject entrypoints = json.getAsJsonObject("entrypoints");
                    if (entrypoints.has("main")) {
                        JsonArray mainArray = entrypoints.getAsJsonArray("main");
                        for (int i = 0; i < mainArray.size(); i++) {
                            String mainClass = mainArray.get(i).getAsString();
                            mainClasses.add(mainClass);
                        }
                    }
                }

                if (json.has("depends")) {
                    JsonObject depends = json.getAsJsonObject("depends");
                    if (depends.has("minecraft")) {
                        mcVersion = depends.get("minecraft").getAsString();
                    }
                }
            } catch (Exception e) {
                // Ignore JSON parsing errors
            }

            String finalMcVersion = mcVersion;
            batch.add(() -> {
                Label title = new BoundLabel(Lang.getBinding("service.analysis.is-fabric-mod"));
                consumer.appendSummary(title);

                if (!finalMcVersion.isEmpty()) {
                    consumer.appendSummary(new Label(Lang.getBinding("service.analysis.minecraft-version").get() + " " + finalMcVersion));
                } else {
                    consumer.appendSummary(new BoundLabel(Lang.getBinding("service.analysis.minecraft-version-unknown")));
                }

                if (!mainClasses.isEmpty()) {
                    consumer.appendSummary(new BoundLabel(Lang.getBinding("service.analysis.entry-points")));
                    for (String mainClass : mainClasses) {
                        // Try to find the main class in JVM class bundle
                        JvmClassInfo classInfo = findClassInResource(resource, mainClass);
                        if (classInfo != null) {
                            // Found class, create label with icon
                            String classDisplay = textService.getJvmClassInfoTextProvider(workspace, resource,
                                    resource.getJvmClassBundle(), classInfo).makeText();
                            Node classIcon = iconService.getJvmClassInfoIconProvider(workspace, resource,
                                    resource.getJvmClassBundle(), classInfo).makeIcon();
                            Label classLabel = new Label(classDisplay, classIcon);
                            classLabel.setCursor(Cursor.HAND);
                            classLabel.setOnMouseEntered(e -> classLabel.getStyleClass().add(Styles.TEXT_UNDERLINED));
                            classLabel.setOnMouseExited(e -> classLabel.getStyleClass().remove(Styles.TEXT_UNDERLINED));
                            classLabel.setOnMouseClicked(e -> actions.gotoDeclaration(workspace, resource,
                                    resource.getJvmClassBundle(), classInfo));
                            consumer.appendSummary(classLabel);
                        }
                    }
                } else {
                    consumer.appendSummary(new BoundLabel(Lang.getBinding("service.analysis.entry-points.none")));
                }
            });

        }

        // 2. Try to find Forge mod information
        FileInfo forgeFileInfo = resource.getFileBundle().get("mcmod.info");
        if (forgeFileInfo != null) {
            foundAny = true;
            String mcVersion = "";
            List<String> corePlugins = new ArrayList<>();

            try {
                // Parse mcmod.info to get mcversion
                String jsonText = forgeFileInfo.asTextFile().getText();
                JsonArray modArray = JsonParser.parseString(jsonText).getAsJsonArray();
                if (!modArray.isEmpty()) {
                    JsonObject modInfo = modArray.get(0).getAsJsonObject();
                    if (modInfo.has("mcversion")) {
                        mcVersion = modInfo.get("mcversion").getAsString();
                    }
                }
            } catch (Exception e) {
                // Ignore JSON parsing errors
            }

            // Parse manifest to get FMLCorePlugin
            FileInfo manifestFileInfo = resource.getFileBundle().get("META-INF/MANIFEST.MF");
            if (manifestFileInfo != null) {
                try {
                    String manifest = manifestFileInfo.asTextFile().getText();
                    Manifest mf = new Manifest(new ByteArrayInputStream(manifest.getBytes()));
                    String corePlugin = mf.getMainAttributes().getValue("FMLCorePlugin");
                    if (corePlugin != null && !corePlugin.isEmpty()) {
                        corePlugins.add(corePlugin);
                    }
                } catch (Exception e) {
                    // Ignore manifest parsing errors
                }
            }

            String finalMcVersion = mcVersion;
            batch.add(() -> {
                Label title = new BoundLabel(Lang.getBinding("service.analysis.is-forge-mod"));
                consumer.appendSummary(title);

                if (!finalMcVersion.isEmpty()) {
                    consumer.appendSummary(new Label(Lang.getBinding("service.analysis.minecraft-version").get() + " " + finalMcVersion));
                } else {
                    consumer.appendSummary(new BoundLabel(Lang.getBinding("service.analysis.minecraft-version-unknown")));
                }

                if (!corePlugins.isEmpty()) {
                    consumer.appendSummary(new BoundLabel(Lang.getBinding("service.analysis.entry-points")));
                    for (String corePlugin : corePlugins) {
                        // Try to find the core plugin class in JVM class bundle
                        JvmClassInfo classInfo = findClassInResource(resource, corePlugin);
                        if (classInfo != null) {
                            // Found class, create label with icon
                            String classDisplay = textService.getJvmClassInfoTextProvider(workspace, resource,
                                    resource.getJvmClassBundle(), classInfo).makeText();
                            Node classIcon = iconService.getJvmClassInfoIconProvider(workspace, resource,
                                    resource.getJvmClassBundle(), classInfo).makeIcon();
                            Label classLabel = new Label(classDisplay, classIcon);
                            classLabel.setCursor(Cursor.HAND);
                            classLabel.setOnMouseEntered(e -> classLabel.getStyleClass().add(Styles.TEXT_UNDERLINED));
                            classLabel.setOnMouseExited(e -> classLabel.getStyleClass().remove(Styles.TEXT_UNDERLINED));
                            classLabel.setOnMouseClicked(e -> actions.gotoDeclaration(workspace, resource,
                                    resource.getJvmClassBundle(), classInfo));
                            consumer.appendSummary(classLabel);
                        }
                    }

                } else {
                    consumer.appendSummary(new BoundLabel(Lang.getBinding("service.analysis.entry-points.none")));
                }
            });
        }

        if (!foundAny) {
            batch.add(() -> consumer.appendSummary(new BoundLabel(Lang.getBinding("service.analysis.no-minecraft-mod-found"))));
        }

        batch.execute();
        return foundAny;
    }

    /**
     * Find a class in the resource by its name.
     * Converts dot notation to slash notation for lookup.
     */
    private JvmClassInfo findClassInResource(WorkspaceResource resource, String className) {
        String classPath = className.replace('.', '/');
        return resource.getJvmClassBundle().get(classPath);
    }
}