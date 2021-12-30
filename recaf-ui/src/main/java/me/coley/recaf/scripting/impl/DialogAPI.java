package me.coley.recaf.scripting.impl;

import javafx.stage.FileChooser;
import me.coley.recaf.RecafUI;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;

public class DialogAPI {
    private static Logger logger = Logging.get(DialogAPI.class);

    public static File singleOpenFileDialog() {
        FileChooser chooser = new FileChooser();
        return chooser.showOpenDialog(RecafUI.getWindows().getMainWindow());
    }

    public static File singleSaveFileDialog() {
        FileChooser chooser = new FileChooser();
        return chooser.showSaveDialog(RecafUI.getWindows().getMainWindow());
    }

    public static List<File> multipleOpenFileDialog() {
        FileChooser chooser = new FileChooser();
        return chooser.showOpenMultipleDialog(RecafUI.getWindows().getMainWindow());
    }
}
