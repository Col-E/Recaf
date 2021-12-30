package me.coley.recaf.scripting.impl;

import javafx.stage.Window;
import me.coley.recaf.RecafUI;

public class WindowAPI {
    public static Window getMainWindow() {
        return RecafUI.getWindows().getMainWindow();
    }

    public static Window getConfigWindow() {
        return RecafUI.getWindows().getConfigWindow();
    }
}
