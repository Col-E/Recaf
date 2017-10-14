package me.coley.recaf.ui.component.list;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class OpcodeKeyListener implements KeyListener {
    private final OpcodeList list;
    private boolean control, shift;

    public OpcodeKeyListener(OpcodeList list) {
        this.list = list;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            control = true;
        } else if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            shift = true;
        }
        update();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            control = false;
        } else if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            shift = false;
        }
        update();
    }

    private void update() {
        list.setModifiers(control, shift);
    }

}
