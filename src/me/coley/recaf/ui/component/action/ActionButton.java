package me.coley.recaf.ui.component.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

@SuppressWarnings("serial")
public class ActionButton extends JButton {
    public ActionButton(String text, Runnable r) {
        super(text);
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        });
    }
}
