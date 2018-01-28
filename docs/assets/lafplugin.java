package me.coley.recaf.plugin;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.alee.laf.WebLookAndFeel;

public class MyPlugin implements Plugin {
    public void init() {
        SwingUtilities.invokeLater(new Runnable() {            
            @Override
            public void run() {
                UIManager.installLookAndFeel("WebLookAndFeel", WebLookAndFeel.class.getName());
            }
        });
    }
}