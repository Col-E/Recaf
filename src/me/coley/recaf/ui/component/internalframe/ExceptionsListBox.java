package me.coley.recaf.ui.component.internalframe;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.ui.component.action.ActionButton;
import me.coley.recaf.ui.component.action.ActionTextField;

@SuppressWarnings("serial")
public class ExceptionsListBox extends BasicFrame {
    private static final Color bg = new Color(166, 166, 166);

    public ExceptionsListBox(MethodNode mn) {
        super("Exceptions: " + mn.name);
        setBackground(bg);
        setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
        update(mn);
        setVisible(true);
    }

    private void update(MethodNode mn) {
        getContentPane().removeAll();
        for (int i = 0; i < mn.exceptions.size(); i++) {
            final int j = i;
            String ex = mn.exceptions.get(i);
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.add(new ActionButton("Delete", () -> {
                mn.exceptions.remove(j);
                update(mn);
            }), BorderLayout.WEST);
            panel.add(new ActionTextField(ex, s -> mn.exceptions.set(j, s)), BorderLayout.CENTER);
            add(panel);
        }
        JPanel panel = new JPanel();
        {
            final JTextField text = new JTextField();
            panel.setLayout(new BorderLayout());
            panel.add(new ActionButton("Add", () -> {
                mn.exceptions.add(text.getText());
                setSize(getWidth(), getHeight() + 30);
                update(mn);
            }), BorderLayout.WEST);
            panel.add(text, BorderLayout.CENTER);
            add(panel);
        }
        getContentPane().repaint();
        getContentPane().validate();
    }
}
