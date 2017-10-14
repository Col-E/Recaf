package me.coley.recaf.ui.component.internalframe;

import java.awt.BorderLayout;
import me.coley.recaf.ui.component.panel.DecompilePanel;

@SuppressWarnings("serial")
public class DecompileBox extends BasicFrame {
    public DecompileBox(DecompilePanel value) throws Exception {
        setLayout(new BorderLayout());
        add(value, BorderLayout.CENTER);
        setMaximumSize(null);
        setTitle(value.getTitle());
        setVisible(true);
    }
}
