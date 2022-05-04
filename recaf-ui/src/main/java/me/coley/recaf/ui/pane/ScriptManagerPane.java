package me.coley.recaf.ui.pane;

import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import me.coley.recaf.RecafUI;
import me.coley.recaf.scripting.Script;
import me.coley.recaf.scripting.ScriptResult;
import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.ui.docking.RecafDockingManager;
import me.coley.recaf.ui.util.Animations;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.MainMenu;
import me.coley.recaf.util.DesktopUtil;
import me.coley.recaf.util.Directories;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class ScriptManagerPane extends BorderPane {
    private static final ScriptManagerPane instance = new ScriptManagerPane();

    private final VBox scriptsList = new VBox();
    private final ScrollPane scrollPane = new ScrollPane(scriptsList);

    private static final ExecutorService THREAD_POOL = Executors.newSingleThreadExecutor((runnable) -> {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        return t;
    });

    // Set up a watcher service to monitor changes in the scripts directory
    static {
        THREAD_POOL.submit(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                Directories.getScriptsDirectory().register(watchService, ENTRY_MODIFY, ENTRY_DELETE);
                while (!Thread.interrupted()) {
                    WatchKey wk = watchService.take();
                    if (!wk.pollEvents().isEmpty())
                        Platform.runLater(instance::populateScripts);
                    if (!wk.reset())
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    // No public construction
    private ScriptManagerPane() {
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        setCenter(scrollPane);

        BorderPane actionPane = new BorderPane();
        actionPane.setPadding(new Insets(2, 2, 2, 2));

        HBox left = new HBox();
        left.setSpacing(4);

        Button newScript = new Button();
        newScript.textProperty().bind(Lang.getBinding("menu.scripting.new"));
        newScript.setGraphic(Icons.getIconView(Icons.PLUS));
        newScript.setOnAction(event -> createNewScript());

        Button browseScripts = new Button();
        browseScripts.textProperty().bind(Lang.getBinding("menu.scripting.browse"));
        browseScripts.setGraphic(Icons.getIconView(Icons.FOLDER));
        browseScripts.setOnAction(event -> browseScripts());

        left.getChildren().addAll(newScript, browseScripts);

        Button refresh = new Button();
        refresh.textProperty().bind(Lang.getBinding("menu.scripting.refresh"));
        refresh.setGraphic(Icons.getIconView(Icons.ACTION_SEARCH));
        refresh.setOnAction(event -> populateScripts());
        refresh.setAlignment(Pos.CENTER_RIGHT);

        actionPane.setLeft(left);
        actionPane.setRight(refresh);

        setBottom(actionPane);

        populateScripts();
    }

    private void populateScripts() {
        scriptsList.getChildren().clear();

        List<Script> scripts = Script.getAvailableScripts();

        if (scripts == null || scripts.isEmpty()) {
            Label label = new Label();
            label.textProperty().bind(Lang.getBinding("menu.scripting.none-found"));
            label.setAlignment(Pos.CENTER);
            label.getStyleClass().addAll("h2", "b");
            scrollPane.setContent(label);
            MainMenu.getInstance().updateScriptMenu(null);
            return;
        }

        List<MenuItem> scriptMenuItems = new ArrayList<>();
        for (Script script : scripts) {
            addScript(script);

            MenuItem item = new MenuItem(script.getName());
            item.setGraphic(Icons.getIconView(Icons.PLAY));
            item.setOnAction(event -> script.execute());
            scriptMenuItems.add(item);
        }

        MainMenu.getInstance().updateScriptMenu(scriptMenuItems);
        scrollPane.setContent(scriptsList);
    }

    private void showScriptEditor(ScriptEditorPane pane) {
        RecafDockingManager docking = RecafDockingManager.getInstance();
        DockTab scriptEditorTab = docking.createTab(() -> new DockTab(Lang.getBinding("menu.scripting.editor"), pane));
        scriptEditorTab.setGraphic(Icons.getIconView(Icons.CODE));
        pane.setTab(scriptEditorTab);
        scriptEditorTab.select();
        RecafUI.getWindows().getMainWindow().requestFocus();
    }

    private void createNewScript() {
        String metadataTemplate =
                "//==Metadata==\n" +
                "// @name New Script\n" +
                "// @description Script Description\n" +
                "// @version 1.0\n" +
                "// @author Script Author\n" +
                "// ==/Metadata==\n\n";
        ScriptEditorPane scriptEditor = new ScriptEditorPane();
        scriptEditor.setText(metadataTemplate);
        showScriptEditor(scriptEditor);
    }

    private void editScript(Script script) {
        ScriptEditorPane scriptEditor = new ScriptEditorPane();
        scriptEditor.openFile(script.getPath());
        showScriptEditor(scriptEditor);
    }

    /**
     * Open the 'scripts' directory in the file explorer.
     */
    private void browseScripts() {
        try {
            DesktopUtil.showDocument(Directories.getScriptsDirectory().toUri());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Label makeAttribLabel(StringBinding langBinding, String text) {
        Label label = new Label(text);
        if(langBinding != null) {
            label.textProperty().bind(new StringBinding() {
                {
                    bind(langBinding);
                }

                @Override
                protected String computeValue() {
                    return String.format("  • %s: %s", langBinding.get(), text);
                }
            });
        }
        label.setMinSize(350, 20);
        return label;
    }

    private void addScript(Script script) {
        BorderPane scriptRow = new BorderPane();
        scriptRow.setPadding(new Insets(4, 4, 4, 4));

        Label nameLabel = new Label(script.getName());
        nameLabel.setMinSize(350, 20);
        nameLabel.getStyleClass().addAll("b", "h1");

        VBox info = new VBox();
        info.getChildren().add(nameLabel);

        String description = script.getTag("description");
        String author = script.getTag("author");
        String version = script.getTag("version");
        String url = script.getTag("url");

        if(description != null)
            info.getChildren().add(makeAttribLabel(null, description));
        if(author != null)
            info.getChildren().add(makeAttribLabel(Lang.getBinding("menu.scripting.author"), author));
        if(version != null)
            info.getChildren().add(makeAttribLabel(Lang.getBinding("menu.scripting.version"), version));
        if(url != null) {
            info.getChildren().add(makeAttribLabel(new StringBinding() {
                @Override
                protected String computeValue() {
                    return "URL";
                }
            }, url));
        }

        VBox actions = new VBox();
        actions.setSpacing(4);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button executeButton = new Button();
        executeButton.textProperty().bind(Lang.getBinding("menu.scripting.execute"));
        executeButton.setGraphic(Icons.getIconView(Icons.PLAY));

        executeButton.setOnAction(event -> {
            ScriptResult result = script.execute();
            if(result.wasSuccess())
                Animations.animateSuccess(scrollPane, 1000);
            else
                Animations.animateFailure(scrollPane, 1000);
        });
        executeButton.setPrefSize(130, 30);

        Button editButton = new Button();
        editButton.textProperty().bind(Lang.getBinding("menu.scripting.edit"));
        editButton.setGraphic(Icons.getIconView(Icons.ACTION_EDIT));

        editButton.setOnAction(event -> editScript(script));
        editButton.setPrefSize(130, 30);

        actions.getChildren().addAll(executeButton, editButton);

        Separator separator = new Separator(Orientation.HORIZONTAL);
        separator.prefWidthProperty().bind(scrollPane.widthProperty());

        scriptRow.setLeft(info);
        scriptRow.setRight(actions);

        scriptRow.prefWidthProperty().bind(widthProperty());
        scriptsList.getChildren().addAll(scriptRow, separator);
    }

    /**
     * @return The instance
     */
    public static ScriptManagerPane getInstance() {
        return instance;
    }
}