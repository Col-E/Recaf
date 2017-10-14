package me.coley.recaf.ui.component;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Interface with defaults for all but release. Makes things implementing mouse
 * listener look a little cleaner <i>(Since the stub event receivers won't need
 * to be declared)</i>
 *
 * @author Matt
 */
public interface ReleaseListener extends MouseListener {
	@Override
default void mouseClicked(MouseEvent e) {}

	@Override
default void mousePressed(MouseEvent e) {}

	@Override
default void mouseEntered(MouseEvent e) {}

	@Override
default void mouseExited(MouseEvent e) {}
}
